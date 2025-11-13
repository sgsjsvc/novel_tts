package novel.tts.novel_tts.service.ipml;

import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.ChapterMapper;
import novel.tts.novel_tts.service.WavWatcherService;
import novel.tts.novel_tts.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@Slf4j
public class WavWatcherServiceIpml implements WavWatcherService {

    @Autowired
    private ChapterMapper chapterMapper;

    private static final Path AUDIO_ROOT = Paths.get("temp/output/audio");
    private static final Path GEMINI_TXT_ROOT = Paths.get("temp/output/geminiTxt");
    private static final Path TXT_ROOT = Paths.get("temp/output/txt");

    @Override
    public void initialScan() {
        log.info("🔍 启动时全量扫描...");
        try (Stream<Path> novels = Files.list(AUDIO_ROOT)) {
            novels.filter(Files::isDirectory).forEach(novelDir -> {
                try (Stream<Path> chapters = Files.list(novelDir)) {
                    chapters.filter(Files::isDirectory)
                            .forEach(this::checkAndUpdateStatus);
                } catch (IOException e) {
                    log.error("扫描小说目录失败: {}", novelDir, e);
                }
            });
        } catch (IOException e) {
            log.error("全量扫描失败", e);
        }
        log.info("✅ 全量扫描完成");
    }

    @Override
    public void startWatching() {
        while (true) { // 自动重启机制
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

                registerAllDirs(AUDIO_ROOT, watchService);
                log.info("📡 文件监听已启动: {}", AUDIO_ROOT.toAbsolutePath());

                while (true) {
                    WatchKey key = watchService.take(); // 阻塞等待事件
                    Path dir = (Path) key.watchable();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path changed = dir.resolve((Path) event.context()).toAbsolutePath().normalize();

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            if (Files.isDirectory(changed)) {
                                // 新建目录时递归注册
                                registerAllDirs(changed, watchService);
                                log.info("🆕 新目录已注册监听: {}", changed);
                            } else if (changed.toString().endsWith(".wav")) {
                                checkAndUpdateStatus(changed.getParent());
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY
                                && changed.toString().endsWith(".wav")) {
                            checkAndUpdateStatus(changed.getParent());
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            log.debug("🗑 文件被删除: {}", changed);
                        }
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        log.warn("⚠️ 监听失效: {}", dir);
                    }
                }

            } catch (Exception e) {
                log.error("监听线程异常，即将重启...", e);
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * 递归注册目录监听
     */
    private void registerAllDirs(Path start, WatchService ws) {
        try (Stream<Path> dirs = Files.walk(start)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                try {
                    dir.register(ws,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE);
                } catch (IOException e) {
                    log.error("注册监听失败: {}", dir, e);
                }
            });
        } catch (IOException e) {
            log.error("递归注册目录失败: {}", start, e);
        }
    }

    /**
     * 检查章节目录对应的状态并更新数据库
     */
    private void checkAndUpdateStatus(Path audioChapterDir) {
        try {
            Path novelName = audioChapterDir.getParent().getFileName();
            Path chapterName = audioChapterDir.getFileName();

            Path geminiTxtPath = GEMINI_TXT_ROOT.resolve(novelName).resolve(chapterName + ".txt");
            Path txtPath = TXT_ROOT.resolve(novelName).resolve(chapterName + ".txt");

            if (!Files.exists(geminiTxtPath)) {
                log.warn("❌ 源文本不存在: {}", geminiTxtPath);
                return;
            }
            if (!Files.exists(txtPath)) {
                log.warn("⚠️ 输出文本不存在: {}", txtPath);
                return;
            }

            long wavCount = FileUtils.countWavFiles(audioChapterDir);
            long txtLines = FileUtils.countTxtLines(geminiTxtPath);

            int status = (wavCount == 0) ? 0 : (wavCount < txtLines ? 2 : 1);

            String normalizedPath = txtPath.toString().replace("\\", "/");

            if (chapterMapper.existsByTxtPath(normalizedPath) > 0) {
                chapterMapper.updateChapterStatus(normalizedPath, status);
                log.info("📘 [{}] 状态更新成功 => {}, wav数量={}, txt行数={}",
                        txtPath.getFileName(), status, wavCount, txtLines);
            } else {
                log.info("⚠️ 数据库未找到章节记录: {}", normalizedPath);
            }

        } catch (Exception e) {
            log.error("章节状态检测失败: {}", audioChapterDir, e);
        }
    }
}
