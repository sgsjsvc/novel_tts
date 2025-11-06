package novel.tts.novel_tts.task;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.service.FolderWatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件夹及子文件夹监听工具类
 * 启动后自动监听文件变化（创建/删除）
 */
@Component
@Slf4j
public class FolderWatcher {
    @Autowired
    private FolderWatcherService folderWatcherService;

    @Value("${folder.watch.path:temp}")  // 默认相对路径 ./temp
    private String folderPath;

    private final Map<WatchKey, Path> watchKeyPathMap = new HashMap<>();
    private WatchService watchService;
    private Thread watcherThread;
    private volatile boolean running = true;

    /**
     * 项目启动完成后自动执行
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init(ApplicationReadyEvent event) {
        watcherThread = new Thread(this::startWatching, "Folder-Watcher-Thread");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    /**
     * 核心监听逻辑
     */
    private void startWatching() {
        try {
            Path rootPath = getAbsolutePath(folderPath);
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }

            watchService = FileSystems.getDefault().newWatchService();

            log.info("📁 开始监听文件夹及子文件夹：{}", rootPath.toAbsolutePath());
            registerAllDirectories(rootPath, watchService);

            while (running) {
                WatchKey key = watchService.take(); // 阻塞等待事件
                Path dir = watchKeyPathMap.get(key);
                if (dir == null) {
                    key.reset();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    Path name = (Path) event.context();
                    Path child = dir.resolve(name);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (Files.isDirectory(child)) {
                            registerAllDirectories(child, watchService);
                        } else {
                            folderWatcherService.insert(getRelativePath(child.toAbsolutePath()));
                            log.info("🟢 新增文件成功: {}", getRelativePath(child.toAbsolutePath()));
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        folderWatcherService.delete(getRelativePath(child.toAbsolutePath()));
                        log.info("🔴 删除文件成功: {}", getRelativePath(child.toAbsolutePath()));
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    watchKeyPathMap.remove(key);
                    if (watchKeyPathMap.isEmpty()) {
                        log.warn("⚠️ 所有监听目录失效，停止监听。");
                        break;
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            log.error("文件夹监听异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 项目关闭时释放资源
     */
    @PreDestroy
    public void onShutdown() {
        running = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            log.error("关闭 WatchService 失败: {}", e.getMessage());
        }
        log.info("🛑 文件夹监听线程已关闭");
    }

    /**
     * 获取相对路径对应的绝对路径
     */
    private Path getAbsolutePath(String path) {
        return Paths.get(System.getProperty("user.dir")).resolve(path).toAbsolutePath();
    }

    /**
     * 将绝对路径转换为相对于项目根目录的相对路径
     */
    private String getRelativePath(Path absolutePath) {
        Path basePath = Paths.get(System.getProperty("user.dir")); // 项目根目录
        try {
            // 返回使用 / 作为分隔符的相对路径
            return basePath.relativize(absolutePath.toAbsolutePath()).toString().replace("\\", "/");
        } catch (IllegalArgumentException e) {
            // 如果路径不在当前目录下，则返回文件名
            return absolutePath.getFileName().toString();
        }
    }

    /**
     * 注册所有子目录
     */
    private void registerAllDirectories(Path start, WatchService watchService) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir, watchService);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 注册单个目录监听
     */
    private void registerDirectory(Path dir, WatchService watchService) throws IOException {
        WatchKey key = dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE
        );
        watchKeyPathMap.put(key, dir);
        log.info("🔍 已监听目录：{}", dir.toAbsolutePath());
    }


}
