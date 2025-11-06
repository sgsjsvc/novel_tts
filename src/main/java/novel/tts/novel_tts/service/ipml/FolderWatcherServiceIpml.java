package novel.tts.novel_tts.service.ipml;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.FolderWatcherMapper;
import novel.tts.novel_tts.service.FolderWatcherService;
import novel.tts.novel_tts.util.NovelCleanerAndSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class FolderWatcherServiceIpml implements FolderWatcherService {

    // 文件输出路径
    @Value("${folder.watch.outputFlowPath:temp/output/txt/}")
    private String outputFlowPath;
    // 待处理目录
    @Value("${folder.watch.ProcessingDirectory:downloads}")
    private String ProcessingDirectory;

    @Autowired
    private FolderWatcherMapper folderWatcherMapper;


    @Override
    public void insert(String relativePath) {
        Path path = Paths.get(relativePath);
        log.info("📄 新增文件: {}", path);

        String fileName = path.getFileName().toString();
        String parentDirectory = path.getParent() != null ? path.getParent().getFileName().toString() : "";
        log.info("📁 上级目录: {}", parentDirectory);

        // 只处理 downloads 目录
        if (parentDirectory.equals(ProcessingDirectory)) {
            try {
                // ✅ 等待文件写入完成
                waitForFileReady(path);

                Path absolutePath = path.toAbsolutePath();
                String fileNameNoExt = fileName.replaceAll("\\.txt$", "");
                String outputDir = outputFlowPath + fileNameNoExt + "/";
                Files.createDirectories(Paths.get(outputDir));

                NovelCleanerAndSplitter.processNovel(absolutePath.toString(), outputDir);
                log.info("✨ 小说清洗与分章节完成: {}", outputDir);

            } catch (Exception e) {
                log.error("❌ 小说清洗失败: {}", e.getMessage(), e);
            }
        }

        folderWatcherMapper.insertUrl(fileName, parentDirectory, relativePath);
        log.info("💾 插入数据库成功");
    }

    /**
     * 等待文件写入完成（文件大小稳定）
     */
    private void waitForFileReady(Path path) throws InterruptedException, IOException {
        long lastSize = -1;
        int stableCount = 0;

        // 最多等待 10 秒
        for (int i = 0; i < 20; i++) {
            if (!Files.exists(path)) {
                Thread.sleep(500);
                continue;
            }

            long currentSize = Files.size(path);
            if (currentSize == lastSize) {
                stableCount++;
                if (stableCount >= 2) { // 连续两次相同，认为写入完成
                    log.info("✅ 文件已稳定，准备处理: {}", path);
                    return;
                }
            } else {
                stableCount = 0; // 大小变化，重新计数
            }

            lastSize = currentSize;
            Thread.sleep(500);
        }

        log.warn("⚠️ 文件长时间未稳定，强制继续处理: {}", path);
    }


    @Override
    public void delete(String child) {
        log.info("🗑️ 等待删除数据：{}",child);
        Path path = Paths.get(child);
            if (Files.isDirectory(path)) {
                log.info("📁 等待删除文件夹：{}",child);
                folderWatcherMapper.deleteByPrefix(child);
            } else if (Files.isRegularFile(path)) {
                log.info("📄 等待删除文件：{}",child);
                folderWatcherMapper.delete(child);
            } else {
                log.info("❓ 不是普通文件或文件夹（可能是符号链接、管道等）：{}",child);
                folderWatcherMapper.deleteByPrefix(child);
            }
        }
}

