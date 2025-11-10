package novel.tts.novel_tts.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 情绪推理 + 自动音频下载保存工具
 */
@Slf4j
@Component
public class InferEmotionClient {

    @Value("${emotion.infer.url}")
    private String inferUrl;

    @Value("${emotion.save.dir:temp/output/audio}") // 默认保存目录
    private String saveDir1;

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ 用原子计数器确保文件名自增且线程安全
    private static final AtomicInteger FILE_COUNTER = new AtomicInteger(0);

    /**
     * 执行情绪推理并自动下载音频
     * @param text 待合成文本
     * @return 保存的本地文件路径
     */
    public String infer(String text,String file,String Model) {
        String saveDir=saveDir1+"/"+file;
        try {
            // 1️⃣ 构造请求体
            Map<String, Object> body = new HashMap<>();
            body.put("app_key", "");
            body.put("dl_url", "");
            body.put("version", "v2");
            body.put("model_name", Model != null ? Model : "花火");
            body.put("prompt_text_lang", "中文");
            body.put("emotion", "默认");
            body.put("text", text);
            body.put("text_lang", "中文");
            body.put("top_k", 10);
            body.put("top_p", 1);
            body.put("temperature", 1);
            body.put("text_split_method", "按标点符号切");
            body.put("batch_size", 1);
            body.put("batch_threshold", 0.75);
            body.put("split_bucket", true);
            body.put("speed_facter", 1);
            body.put("fragment_interval", 0.3);
            body.put("media_type", "wav");
            body.put("parallel_infer", true);
            body.put("repetition_penalty", 1.35);
            body.put("seed", -1);
            body.put("sample_steps", 16);
            body.put("if_sr", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // 2️⃣ 发起推理请求
            ResponseEntity<Map> response = restTemplate.exchange(
                    inferUrl, HttpMethod.POST, request, Map.class);

            if (response.getBody() == null || !"合成成功".equals(response.getBody().get("msg"))) {
                log.error("❌ 推理失败: {}", response.getBody());
                return null;
            }

            // 3️⃣ 获取音频 URL
            String audioUrl = (String) response.getBody().get("audio_url");
            if (audioUrl == null || audioUrl.isEmpty()) {
                log.error("❌ 音频 URL 为空");
                return null;
            }

            // 修复 0.0.0.0 问题
            audioUrl = audioUrl.replace("0.0.0.0", "localhost");

            // 4️⃣ 下载音频文件
            byte[] audioBytes = restTemplate.getForObject(audioUrl, byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                log.error("❌ 音频下载失败");
                return null;
            }

            // 5️⃣ 生成安全保存目录
            Path dir = Paths.get(saveDir);
            Files.createDirectories(dir);

            // 6️⃣ 按顺序生成递增文件名
            int index = FILE_COUNTER.incrementAndGet();
            String fileName = String.format("%03d.wav", index);
            Path outputPath = dir.resolve(fileName);

            // 7️⃣ 写入文件
            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                fos.write(audioBytes);
            }

            log.info("✅ [{}] 情绪推理成功 -> {}", fileName, outputPath);
            return outputPath.toAbsolutePath().toString();

        } catch (Exception e) {
            log.error("❌ [InferEmotion] 调用失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
