package novel.tts.novel_tts.util;

import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.PersonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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

    // 匹配格式：姓名(性别)：台词
    private static final Pattern LINE_PATTERN = Pattern.compile("^(.+?)\\((男|女|未知)\\)：(.+)$");

    /**
     * 从文件逐行读取并自动调用 inferEmotionClient
     *
     * @param filePath txt 文件路径
     */
    public void processFile(String filePath, String table, String file) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.error("❌ 文件不存在: {}", filePath);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                if (line.isEmpty()) continue;

                Matcher matcher = LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String name = matcher.group(1).trim();
                    String gender = matcher.group(2).trim();
                    String content = matcher.group(3).trim();

                    log.info("🎬 第{}行 -> [{}]({})：{}", lineNum, name, gender, content);

                    String characterName = personMapper.getCharacterName(table, name);
                    log.info("characterName:{}", characterName);
                    // 🔹 调用推理接口（你可替换 emotion 参数）
                    String response = inferEmotionClient.infer(content, file, characterName);

                    if (response != null) {
                        log.info("✅ [{}] 推理完成，返回：{}", name, response);
                    } else {
                        log.warn("⚠️ [{}] 推理失败", name);
                    }

                    // 这里可适当延时避免接口过载
                    Thread.sleep(200);
                } else {
                    log.warn("⚠️ 第{}行格式不符，跳过: {}", lineNum, line);
                }
            }

        } catch (IOException e) {
            log.error("❌ 读取文件失败: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 线程中断: {}", e.getMessage(), e);
        }
    }
}
