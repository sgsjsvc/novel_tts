package novel.tts.novel_tts.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.FolderWatcherMapper;
import novel.tts.novel_tts.service.FolderWatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * 文件夹扫描工具类
 * 启动时自动执行，打印指定目录及其子目录下的所有文件相对路径
 */
@Slf4j
@Component
public class FolderScanner {

    /**
     * 配置文件中定义的目标文件夹相对路径
     * 示例：folder.path=temp
     */
    @Value("${folder.path:temp}")  // 默认扫描 ./temp 文件夹
    private String folderPath;


    @Autowired
    private FolderWatcherMapper folderWatcherMapper;
    @Autowired
    private FolderWatcherService folderWatcherService;
    /**
     * 启动时自动执行
     */
    @PostConstruct
    public void scanFilesOnStartup() {
        log.info("📁 开始扫描文件夹: {}", folderPath);

        // 当前项目运行目录
        Path currentDir = Paths.get(System.getProperty("user.dir"));

        // 目标根目录（相对路径）
        Path rootPath = currentDir.resolve(folderPath);

        if (!Files.exists(rootPath)) {
            log.warn("⚠️ 指定文件夹不存在: {}", rootPath);
            return;
        }

        folderWatcherMapper.deleteAll();
        log.info("🗑️ 清空数据库表");

        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.filter(Files::isRegularFile) // 只输出文件
                    .forEach(path -> {
                        // 输出相对路径（相对于 rootPath）
                        Path relativePath = Path.of(folderPath +"\\"+ rootPath.relativize(path));
                        folderWatcherService.insert(String.valueOf(relativePath));
                        log.info("🗂️ 文件: {}", relativePath);
                    });
        } catch (IOException e) {
            log.error("扫描文件夹时出错: {}", e.getMessage(), e);
        }

        log.info("✅ 文件夹扫描完成。");
    }
}
