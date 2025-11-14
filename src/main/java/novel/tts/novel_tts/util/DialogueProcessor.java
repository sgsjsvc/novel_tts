package novel.tts.novel_tts.util;

import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.PersonMapper;
import novel.tts.novel_tts.service.ParsingProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按行读取脚本文本并逐条调用 InferEmotionClient 推理生成音频
 * 示例行格式：
 * 陈林(男)：局长，你去我们办公室有事么？
 * 旁白(未知)：松海公安局。
 */
@Slf4j
@Component
public class DialogueProcessor {

    // 引入你写好的 InferEmotionClient
    @Autowired
    private InferEmotionClient inferEmotionClient;
    @Autowired
    private PersonMapper personMapper;
    @Autowired
    private ParsingProgressService parsingProgressService;

    @Value("${emotion.save.dir:temp/output/audio}")
    private String audioSaveDir;

    // 匹配格式：姓名(性别)：台词
    private static final Pattern LINE_PATTERN = Pattern.compile("^(.+?)\\((男|女|未知)\\)：(.+)$");

    /**
     * 从文件逐行读取并自动调用 inferEmotionClient
     *
     * @param filePath txt 文件路径
     */
    public void processFile(String filePath, String table, String file, String jobId) {
        // --- Start: Pre-emptive Deletion and Counter Reset ---
        try {
            // 1. Reset the counter in InferEmotionClient
            inferEmotionClient.resetCounter();

            // 2. Construct the specific audio output directory for the chapter
            Path chapterAudioDir = Paths.get(audioSaveDir, file);

            // 3. Delete existing files in the directory
            if (Files.exists(chapterAudioDir)) {
                log.info("ℹ️ 正在清空旧的音频文件于: {}", chapterAudioDir);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(chapterAudioDir)) {
                    for (Path entry : stream) {
                        Files.delete(entry);
                    }
                }
                log.info("✅ 成功清空目录: {}", chapterAudioDir);
            }
        } catch (IOException e) {
            log.error("❌ 清空旧音频文件时出错: {}", e.getMessage(), e);
            parsingProgressService.failTask(jobId, "清空旧音频文件失败: " + e.getMessage());
            return;
        }
        // --- End: Pre-emptive Deletion and Counter Reset ---

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.error("❌ 文件不存在: {}", filePath);
            parsingProgressService.failTask(jobId, "输出文件不存在: " + filePath);
            return;
        }

        try {
            List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int totalLines = allLines.size();
            parsingProgressService.updateProgress(jobId, 0, totalLines);
            AtomicInteger completedLines = new AtomicInteger(0);

            for (int i = 0; i < totalLines; i++) {
                String line = allLines.get(i).trim();
                int lineNum = i + 1;

                if (line.isEmpty()) {
                    completedLines.incrementAndGet();
                    parsingProgressService.updateProgress(jobId, completedLines.get(), totalLines);
                    continue;
                }

                Matcher matcher = LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String name = matcher.group(1).trim();
                    String gender = matcher.group(2).trim();
                    String content = matcher.group(3).trim();

                    log.info("🎬 第{}行 -> [{}]({})：{}", lineNum, name, gender, content);

                    String characterName = personMapper.getCharacterName(table, name);
                    log.info("characterName:{}", characterName);
                    String response = inferEmotionClient.infer(content, file, characterName);

                    if (response != null) {
                        log.info("✅ [{}] 推理完成，返回：{}", name, response);
                    } else {
                        log.warn("⚠️ [{}] 推理失败", name);
                    }

                    Thread.sleep(200);
                } else {
                    log.warn("⚠️ 第{}行格式不符，跳过: {}", lineNum, line);
                }

                completedLines.incrementAndGet();
                parsingProgressService.updateProgress(jobId, completedLines.get(), totalLines);
            }
            parsingProgressService.completeTask(jobId);
            log.info("✅ 任务ID: {} - 音频生成全部完成", jobId);

        } catch (IOException e) {
            log.error("❌ 读取文件失败: {}", e.getMessage(), e);
            parsingProgressService.failTask(jobId, "读取文件失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 线程中断: {}", e.getMessage(), e);
            parsingProgressService.failTask(jobId, "线程中断: " + e.getMessage());
        }
    }
}
