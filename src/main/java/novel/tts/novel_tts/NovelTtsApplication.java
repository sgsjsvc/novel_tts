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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@MapperScan("novel.tts.novel_tts.mapper")
@SpringBootApplication
@EnableAsync
@Slf4j
public class NovelTtsApplication {

    private Process soNovelProcess;
    private Process pythonBatProcess;

    @Value("${so-novel.novelPath}")
    private String novelPath;

    private String jarPath;
    private int appPort;

    public static void main(String[] args) {
        SpringApplication.run(NovelTtsApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadConfig();
        startSoNovelJar();
        startPythonByBat();
    }

    /** =============================
     * 读取 config.ini
     * ============================= */
    private void loadConfig() {
        try {
            String base = System.getProperty("user.dir") + novelPath;
            String configPath = base + "/config.ini";
            jarPath = base + "/so-novel.jar";

            log.info("配置文件路径: {}", configPath);
            log.info("JAR 路径: {}", jarPath);

            Wini ini = new Wini(new File(configPath));
            appPort = ini.get("web", "port", int.class);

            log.info("Web端口: {}", appPort);

        } catch (Exception e) {
            log.error("读取 config.ini 失败: {}", e.getMessage());
        }
    }

    /** =============================
     * 启动 so-novel.jar
     * ============================= */
    private void startSoNovelJar() {
        try {
            if (isPortInUse(appPort)) {
                log.warn("端口 {} 已占用，正在释放...", appPort);
                killProcessByPort(appPort);
                Thread.sleep(800);
            }

            File workDir = new File(new File(jarPath).getParent());

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-Djava.awt.headless=true",
                    "-jar",
                    jarPath
            );

            pb.directory(workDir);
            pb.redirectErrorStream(true);

            soNovelProcess = pb.start();
            log.info("so-novel.jar 已启动");

            pipeOutput("[SO-NOVEL]", soNovelProcess);

        } catch (Exception e) {
            log.error("启动 so-novel.jar 失败: {}", e.getMessage());
        }
    }

    /** =============================
     * 启动 Python（使用 bat）
     * ============================= */
    private void startPythonByBat() {
        try {
            String baseDir = System.getProperty("user.dir") + novelPath + "/GPT-SoVITS/";
            File workDir = new File(baseDir);

            File batFile = new File(baseDir + "gsvi.bat");
            if (!batFile.exists()) {
                log.error("找不到 gsvi.bat 文件：{}", batFile.getAbsolutePath());
                return;
            }

            log.info("正在启动 gsvi.bat...");

            // 使用 cmd /c 来执行 bat 文件
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c", batFile.getAbsolutePath()
            );

            pb.directory(workDir);
            pb.redirectErrorStream(true);

            pythonBatProcess = pb.start();
            log.info("gsvi.bat（Python 服务）已启动");

            pipeOutput("[GSVI]", pythonBatProcess);

            // 启动后删除所有音频文件
            deleteAllAudioFiles();
        } catch (Exception e) {
            log.error("启动 gsvi.bat 失败: {}", e.getMessage());
        }
    }

    /** =============================
     * 输出子进程日志
     * ============================= */
    private void pipeOutput(String tag, Process process) {
        Charset charset = isWindows() ? Charset.forName("GBK") : Charset.forName("UTF-8");

        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(tag + " " + line);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    /** =============================
     * Spring Boot 关闭 → 自动结束子进程
     * ============================= */
    @PreDestroy
    public void onShutdown() {
        log.info("正在关闭全部子进程...");

        kill(soNovelProcess, "so-novel.jar");
        kill(pythonBatProcess, "gsvi.bat/Python");
        killPythonByPid();

        log.info("所有子进程已关闭");
    }

    private void kill(Process p, String name) {
        try {
            if (p != null && p.isAlive()) {
                log.info("终止 {}", name);
                p.destroy();
            }
        } catch (Exception ignored) {}
    }

    /** =============================
     * 工具函数：端口检测 & 杀进程
     * ============================= */
    private boolean isPortInUse(int port) {
        try (ServerSocket s = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private void killProcessByPort(int port) {
        try {
            Process p = new ProcessBuilder("cmd.exe", "/c",
                    "netstat -ano | findstr :" + port).start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "GBK"));
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String pid = line.split("\\s+")[4];

                new ProcessBuilder("taskkill", "/F", "/PID", pid).start().waitFor();
            }
        } catch (Exception ignored) {}
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    private void killPythonByPid() {
        try {
            File pidFile = new File(System.getProperty("user.dir") + novelPath + "/GPT-SoVITS/app.pid");
            if (!pidFile.exists()) {
                log.warn("app.pid 不存在，无法结束 python.exe");
                return;
            }

            String pid = Files.readString(pidFile.toPath()).trim();
            log.info("终止 python.exe PID={}", pid);

            new ProcessBuilder("taskkill", "/F", "/PID", pid).start().waitFor();

            // 删除 PID 文件
            pidFile.delete();

        } catch (Exception e) {
            log.error("killPythonByPid 失败：{}", e.getMessage());
        }
    }

    /** =============================
     * 工具函数：删除所有音频文件
     * ============================= */

    public static void deleteAllAudioFiles() {
        Path outputDir = Paths.get(System.getProperty("user.dir"))
                .resolve("GPT-SoVITS/outputs");

        if (!Files.exists(outputDir)) {
            log.warn("音频输出目录不存在，跳过删除: {}", outputDir);
            return;
        }

        List<String> audioExts = Arrays.asList(".wav", ".mp3", ".flac", ".ogg", ".m4a");

        try {
            long deletedCount = Files.walk(outputDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return audioExts.stream().anyMatch(name::endsWith);
                    })
                    .peek(path -> {
                        try {
                            Files.delete(path);
                            log.info("已删除音频文件: {}", path.getFileName());
                        } catch (IOException e) {
                            log.error("删除音频文件失败: {}", path.getFileName(), e);
                        }
                    })
                    .count();

            log.info("音频清理完成，共删除 {} 个文件", deletedCount);

        } catch (IOException e) {
            log.error("遍历音频目录时发生错误: {}", outputDir, e);
            throw new RuntimeException("清理音频文件失败", e);
        }
    }

}
