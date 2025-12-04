package novel.tts.novel_tts.service.ipml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.GeminiCharacterMapper;
import novel.tts.novel_tts.mapper.PersonMapper;
import novel.tts.novel_tts.pojo.*;
import novel.tts.novel_tts.service.GeminiCharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class GeminiCharacterServiceIpml implements GeminiCharacterService {

    @Autowired
    private GeminiCharacterMapper geminiCharacterMapper;
    @Autowired
    private PersonMapper personMapper;

    @Override
    public List<TtsCharacter> getCharacters(String search, String gender, String version) {
        return geminiCharacterMapper.getCharacters(search, gender, version);
    }

    @Override
    public Statistics getCharacterStatistics() {
        // 统计信息
        Map<String, Integer> genderStats = new HashMap<>();
        genderStats.put("男", geminiCharacterMapper.cont("男"));
        genderStats.put("女", geminiCharacterMapper.cont("女"));
        genderStats.put("未知", geminiCharacterMapper.cont("未知"));

        return new Statistics(geminiCharacterMapper.contall(), genderStats);
    }

    @Override
    public int importCharacters(ModelRequest modelRequest) {
        List<String> names = new ArrayList<>();
        log.info("接收到请求：{}", modelRequest);
        try {
            String url = modelRequest.getApiUrl().trim();
            log.info("请求目标 URL: {}", url);

            // ✅ 强制使用 HTTP/1.1
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            // ✅ 构造 JSON
            String json = String.format("{\"version\": \"%s\"}", modelRequest.getVersion());
            log.info("准备发送到 FastAPI 的 JSON：{}", json);

            // ✅ 构建 POST 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            // ✅ 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.info("FastAPI 响应状态码: {}", response.statusCode());
            log.info("FastAPI 响应头: {}", response.headers());
            log.info("FastAPI 响应体: {}", response.body());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode modelsNode = root.get("models");
            if (modelsNode != null && modelsNode.isObject()) {
                Iterator<String> fieldNames = modelsNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String name = fieldNames.next();
                    names.add(name);
                }
            }
            log.info("解析到 {} 个角色", names.size());
            log.info("角色列表: {}", names);


        }
        catch (Exception e) {
            log.error("调用 FastAPI 出错", e);
            return 0;
        }

            log.info("导入角色:{}", names);
            return geminiCharacterMapper.importCharacters(names,modelRequest.getVersion(), LocalDateTime.now());

    }
    @Override
    public List<NovelTable> selectCharacters(String novelname) {
       String tableName=personMapper.getTableName(novelname);
       log.info("获取到的表名:{}", tableName);

        return geminiCharacterMapper.selectModels(tableName);
    }

    @Override
    public List<TableCharacter> selectAllCharacters(String novelname) {
        String tableName=personMapper.getTableName(novelname);
        log.info("获取到的表名:{}", tableName);
        return geminiCharacterMapper.selectAllCharacters(tableName);
    }
}
