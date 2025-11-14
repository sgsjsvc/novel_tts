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
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@MapperScan("novel.tts.novel_tts.mapper")
@SpringBootApplication
@EnableAsync
@Slf4j
public class NovelTtsApplication {

    private Process appProcess;
    private Process batStartProcess;

    @Value("${so-novel.novelPath}")
    private String novelPath;

    private String jarPath;
    private int appPort;

    public static void main(String[] args) {
        SpringApplication.run(NovelTtsApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            String configPath = System.getProperty("user.dir") + novelPath + "/config.ini";
            jarPath = System.getProperty("user.dir") + novelPath + "/so-novel.jar";

            log.info("⚙️ 配置文件路径: {}", configPath);
            log.info("📦 JAR 路径: {}", jarPath);

            Wini ini = new Wini(new File(configPath));
            appPort = ini.get("web", "port", int.class);
            log.info("🌐 so-novel.jar Web 端口: {}", appPort);

        } catch (IOException e) {
            log.error("❌ 读取 config.ini 失败: {}", e.getMessage());
            return;
        }

        try {
            if (isPortInUse(appPort)) {
                log.warn("⚠️ 端口 {} 已被占用，正在释放...", appPort);
                killProcessByPort(appPort);
                Thread.sleep(800);
            }

            // 启动 so-novel.jar
            File workDir = new File(new File(jarPath).getParent());
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(jarPath);

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workDir);
            builder.redirectErrorStream(true);
            appProcess = builder.start();

            log.info("✅ so-novel.jar 已启动");

            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            Charset charset = os.contains("win") ? Charset.forName("GBK") : Charset.forName("UTF-8");

            Thread logThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(appProcess.getInputStream(), charset))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("💡 [so-novel.jar] {}", line);
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

        // 启动 start-app.bat（可选）
        batStartProcess = runBatFile("start-app.bat");
    }

    @PreDestroy
    public void onShutdown() {
        log.info("🛑 ================================");
        log.info("🛑 Spring Boot 开始关闭流程...");
        log.info("🛑 ================================");

        // 关闭 so-novel.jar
        if (appProcess != null && appProcess.isAlive()) {
            log.info("🛑 正在关闭 so-novel.jar...");
            killProcessByPort(appPort);
        }

        // 关闭 start-app.bat
        killBatProcess(batStartProcess, "start-app.bat");

        log.info("✔️ 所有子进程已清理完毕");
    }

    private boolean isPortInUse(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private void killProcessByPort(int port) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) return;

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
                    log.info("⚙️ 正在终止 PID={} 的进程", pid);
                    new ProcessBuilder("taskkill", "/F", "/PID", pid).start().waitFor();
                }
            }
        } catch (Exception e) {
            log.error("❌ killProcessByPort 失败：{}", e.getMessage());
        }
    }

    private Process runBatFile(String batRelativePath) {
        try {
            String baseDir = System.getProperty("user.dir");
            File batFile = new File(baseDir + novelPath + "/" + batRelativePath);

            if (!batFile.exists()) {
                log.warn("⚠️ bat 文件不存在：{}", batFile.getAbsolutePath());
                return null;
            }

            log.info("▶️ 正在启动脚本：{}", batFile.getAbsolutePath());

            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", batFile.getAbsolutePath());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "GBK"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[BAT] {}", line);
                    }
                } catch (IOException ignored) {}
            }).start();

            return process;

        } catch (Exception e) {
            log.error("❌ bat 脚本执行失败：{}", e.getMessage());
            return null;
        }
    }

    private void killBatProcess(Process p, String name) {
        if (p != null && p.isAlive()) {
            log.info("🛑 正在终止 {} ...", name);
            p.destroy();
        }
    }
}
