package novel.tts.novel_tts;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.ini4j.Wini;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 启动类：Spring Boot 启动/关闭时自动管理 so-novel.jar
 */
@MapperScan("novel.tts.novel_tts.mapper")
@SpringBootApplication
@Slf4j
public class NovelTtsApplication {

    private Process appProcess;

    // so-novel.jar 所在目录，可在 application.yml 中配置
    @Value("${so-novel.novelPath}")
    private String novelPath;

    private String jarPath;

    // 从 config.ini 读取的 Web 服务端口
    private int appPort;

    public static void main(String[] args) {
        SpringApplication.run(NovelTtsApplication.class, args);
    }

    /**
     * Spring Boot 启动完成后自动执行
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            // 读取配置文件路径
            String configPath = System.getProperty("user.dir") + novelPath + "/config.ini";
            jarPath = System.getProperty("user.dir") + novelPath + "/so-novel.jar";

            log.info("⚙️ 配置文件路径: {}", configPath);
            log.info( "\uD83D\uDCC4 目标应用路径: {}", jarPath);

            // 使用 ini4j 读取配置
            Wini ini = new Wini(new File(configPath));
            appPort = ini.get("web", "port", int.class);
            log.info("\uD83D\uDCC4 配置文件路径: {}", appPort);

        } catch (IOException e) {
            log.error("读取 config.ini 失败: {}", e.getMessage());
            return;
        }

        try {
            // 检查端口占用
            if (isPortInUse(appPort)) {
                log.warn("⚠️ 端口 {} 已被占用，尝试释放...", appPort);
                killProcessByPort(appPort);
                Thread.sleep(1000);
            }

            // 启动 so-novel.jar
            File workDir = new File(new File(jarPath).getParent());

            // 构建命令：java -jar so-novel.jar
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(jarPath);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workDir);
            builder.redirectErrorStream(true);
            appProcess = builder.start();

            log.info("✅ so-novel.jar 已成功启动。");

            // 根据系统编码选择字符集
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            Charset charset = os.contains("win") ? Charset.forName("GBK") : Charset.forName("UTF-8");

            // 启动守护线程输出子进程日志
            Thread logThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(appProcess.getInputStream(), charset))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("\uD83D\uDCA1 [so-novel.jar] {}", line);
                    }
                } catch (IOException e) {
                    log.error("❌ 读取 so-novel.jar 输出失败：{}", e.getMessage());
                }
            }, "AppJar-Output-Reader");

            logThread.setDaemon(true);
            logThread.start();

        } catch (Exception e) {
            log.error("❌ 启动 so-novel.jar 失败：{}", e.getMessage());
        }
    }

    /**
     * Spring Boot 关闭时停止 so-novel.jar
     */
    @PreDestroy
    public void onShutdown() {
        if (appProcess != null && appProcess.isAlive()) {
            log.info("\uD83D\uDCA1 正在关闭 so-novel.jar...");
            killProcessByPort(appPort);
            log.info("\uD83D\uDD4A\uFE0F so-novel.jar 已成功关闭。");
        }
    }

    /**
     * 判断端口是否被占用
     */
    private boolean isPortInUse(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            return false; // 未占用
        } catch (IOException e) {
            return true; // 被占用
        }
    }

    /**
     * Windows 下通过端口号终止进程
     */
    private void killProcessByPort(int port) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            log.warn("⚠️ 当前系统非 Windows，请手动关闭进程。");
            return;
        }

        try {
            Process p = new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr :" + port)
                    .redirectErrorStream(true)
                    .start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), Charset.forName("GBK")));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 5) {
                    String pid = parts[4];
                    log.info("⚙️ 终止 PID={} 的进程", pid);
                    new ProcessBuilder("taskkill", "/F", "/PID", pid)
                            .start()
                            .waitFor();
                }
            }
            p.waitFor();
        } catch (Exception e) {
            log.error("结束进程失败：{}", e.getMessage());
        }
    }
}
