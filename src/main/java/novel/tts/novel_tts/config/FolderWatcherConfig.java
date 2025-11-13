package novel.tts.novel_tts.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.service.FolderWatcherService;
import novel.tts.novel_tts.service.WavWatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Slf4j
@Order(999)
public class FolderWatcherConfig implements ApplicationRunner {

    @Autowired
    private WavWatcherService wavWatcherService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("🚀 所有模块已加载完毕，开始启动文件监听服务...");

        // 1️⃣ 启动全量扫描线程
        new Thread(() -> {
            wavWatcherService.initialScan();
        }, "Initial-Scan-Thread").start();

        // 2️⃣ 启动实时监听线程
        new Thread(() -> {
            wavWatcherService.startWatching();
        }, "Folder-Watcher-Thread").start();

        log.info("✅ 文件监听服务已在后台启动完成。");
    }
}
