package novel.tts.novel_tts.util;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.PersonMapper;
import novel.tts.novel_tts.mapper.UtilMapper;
import novel.tts.novel_tts.service.ipml.PersonServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import novel.tts.novel_tts.service.ParsingProgressService;
import org.springframework.scheduling.annotation.Async;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class GeminiConcurrentProcessor {
    // 滑动窗口大小和重叠长度
    @Value("${gemini.GeminiConcurrentProcessor.concurrent.WINDOW_SIZE}")
    private int WINDOW_SIZE;
    // 重叠长度
    @Value("${gemini.GeminiConcurrentProcessor.concurrent.OVERLAP}")
    private int OVERLAP;
    // 重叠保留长度
    @Value("${gemini.GeminiConcurrentProcessor.concurrent.OVERLAP_KEEP}")
    private int OVERLAP_KEEP;
    // Gemini API 地址
    @Value("${gemini.GeminiConcurrentProcessor.api.url}")
    private String GEMINI_URL;
    // FastGPT API 地址
    @Value("${fastgpt.api.url:http://anlin.us.kg:33000/api}") // 默认值为FastGPT文档中的地址
    private String FASTGPT_URL;
    // 思考功能 (Gemini相关)
    @Value("${gemini.GeminiConcurrentProcessor.concurrent.THINKING_BUDGET}")
    private int THINKING_BUDGET;
    // 临时目录
    @Value("${gemini.GeminiConcurrentProcessor.concurrent.tempDir:temp/temp}")
    private String tempDir;
    @Value("${gemini.GeminiConcurrentProcessor.concurrent.geminiTxt:temp/output/geminiTxt/}")
    private String geminiTxt; // 通用输出目录，FastGPT也使用
    @Value("${gemini.GeminiConcurrentProcessor.concurrent.geminiInput:temp/output/txt/}")
    private String geminiInput;
    // 当前正在请求的线程数
    private final AtomicInteger activeRequestCount = new AtomicInteger(0);
    // HTTP 客户端
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private UtilMapper utilMapper;

    @Autowired
    private DbFieldUpdater dbFieldUpdater;

    @Autowired
    private PersonServiceImpl personService;

    @Autowired
    private GetTableName getTableName;
    @Autowired
    private PersonMapper personMapper;
    @Autowired
    private DialogueProcessor dialogueProcessor;

    @Autowired
    private ParsingProgressService parsingProgressService;

    @Async
    public void process(String input, String output, String model, String modelVersion, String jobId) throws IOException, InterruptedException {
        // 输入文件
        String inputFile = geminiInput + "/" + input + ".txt";
        log.info("输入文件:{}", inputFile);
        // 输出文件
        String outputFile = geminiTxt + "/" + output + ".txt";
        log.info("输出文件:{}", outputFile);
        // 输入文件所在目录
        String folderName = input.substring(0, input.lastIndexOf("/"));
        log.info("输入文件所在目录:{}", folderName);
        // 构建 API URL 根据模型
        String API_BASE_URL;
        if ("fastgpt".equalsIgnoreCase(model)) {
            API_BASE_URL = FASTGPT_URL; // FastGPT 使用配置的根 URL
            log.info("FastGPT API Base URL:{}", API_BASE_URL);
        } else {
            API_BASE_URL = GEMINI_URL + "/v1beta/models/" + model + ":generateContent";// Gemini 使用模型特定 URL
            log.info("Gemini API Base URL:{}", API_BASE_URL);
        }
        // 创建临时目录
        Files.createDirectories(Paths.get(tempDir));
        log.info("临时目录创建:{}", Files.createDirectories(Paths.get(tempDir)));
        // 读取输入文件
        List<String> allLines = Files.readAllLines(Paths.get(inputFile));
        log.info("输入文件行数：{}", allLines.size());
        // ===== 修复章节名丢失问题 =====
        // 第一行永远是章节名，必须保持原样
        String chapterName = allLines.get(0);
        // 去掉第一行，让后续滑动窗口从第 2 行开始
        List<String> contentLines = allLines.subList(1, allLines.size());


        // 流程1：生成滑动窗口段
        List<List<String>> segments = createSlidingWindowSegments(contentLines);
        parsingProgressService.updateProgress(jobId, 0, segments.size());

        // 流程2：并发处理所有段落
        List<String> tempFiles = processConcurrently(segments, tempDir, API_BASE_URL, model, jobId); // 传递模型名称
        log.info("临时文件路径{}", tempFiles);
        // 流程3：合并临时文件
        mergeSegmentFiles(tempFiles, outputFile,chapterName);
        log.info("合并临时文件完成，生成文件：{}", outputFile);
        // 流程4：表名生成

        String tableName = personMapper.getTableName(folderName);
        if (tableName == null) {
            tableName = getTableName.TableName(folderName);
            log.info("表名生成成功：{}", tableName);
            // 自动创建表
            personMapper.createTableIfNotExists(tableName);
            log.info("✅ 已确认表存在：{}", tableName);

            personMapper.insertNovelTable(folderName, tableName);
            log.info("表名插入数据库完成:{}，{}", folderName, tableName);
        } else {
            log.info("表名已存在：{}", tableName);
        }

        personService.processFile(outputFile, tableName, modelVersion);
        log.info("角色分配完成");

        parsingProgressService.updateStage(jobId, "GENERATING_AUDIO");
        dialogueProcessor.processFile(outputFile, tableName, input, jobId);

    }

    /**
     * 流程1：创建滑动窗口段
     *
     * @param allLines 全部输入行
     * @return 分段后的列表
     */
    private List<List<String>> createSlidingWindowSegments(List<String> allLines) {
        log.info("开始生成滑动窗口段落...");
        // 创建滑动窗口段
        List<List<String>> segments = new ArrayList<>();
        // 窗口起始行索引
        int start = 0;

        while (start < allLines.size()) {
            int end = Math.min(start + WINDOW_SIZE, allLines.size());
            List<String> segment = new ArrayList<>(allLines.subList(start, end));

            // 第一段：保留完整内容（用于后续删除最后5行）
            // 其他段：已经包含了与上一段重叠的开头部分
            segments.add(segment);

            // 移动窗口：向后移动 WINDOW_SIZE - OVERLAP 行
            // 这样下一段的前 OVERLAP 行会与当前段的后 OVERLAP 行重叠
            start = start + WINDOW_SIZE - OVERLAP;
        }

        log.info("生成段落数量:{}", segments.size());
        return segments;
    }

    /**
     * 流程2：并发处理所有段落
     *
     * @param segments 分段列表
     * @param tempDir  临时文件目录
     * @param apiBaseUrl API 的基础 URL
     * @param model 模型名称
     * @return 临时文件路径列表
     */
    private List<String> processConcurrently(List<List<String>> segments, String tempDir, String apiBaseUrl, String model, String jobId) {
        // 构建 API URL 根据模型
        String modelName;
        if ("fastgpt".equalsIgnoreCase(model)) {
            modelName = "fastgpt"; // FastGPT 使用配置的
            log.info("模型提供商:{}", modelName);
        } else {
            modelName = "gemini";// Gemini 使用模型
            log.info("模型提供商:{}", modelName);
        }


        // 创建线程池
        log.info("开始获取并发数数");
        int MAX_CONCURRENT = utilMapper.getMaxConcurrency(modelName);
        log.info("获取最大并发数成功:{}，模型提供商：{}", MAX_CONCURRENT,modelName);
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT);
        log.info("创建线程池，最大并发数：{}", MAX_CONCURRENT);
        // 保存所有 Future
        List<Future<String>> futures = new ArrayList<>();
        AtomicInteger completedCount = new AtomicInteger(0);
        log.info("开始提交所有任务...");
        // 提交所有任务
        for (int i = 0; i < segments.size(); i++) {
            log.info("提交任务：{}", i);
            final int index = i;
            final List<String> segment = segments.get(i);
            final String currentModel = model; // 闭包捕获模型名称

            Future<String> future = executor.submit(() -> {
                try {
                    String text = String.join("\n", segment);
                    log.info("段落 {} 请求开始...", index);

                    log.info("开始获取api-key");
                    String API_KEY = utilMapper.getToken(modelName);
                    log.info("模型提供商:{}", modelName);
                    log.info("获取api-key成功:{}", API_KEY);

                    //MYSQL线程数记录+1
                    dbFieldUpdater.updateField("api_token", "alive_thread", 1, "token", API_KEY);
                    log.info("MYSQL线程数记录+1");
                    //MYSQL请求次数记录+1
                    dbFieldUpdater.updateField("api_token", "request_frequency", 1, "token", API_KEY);
                    log.info("MYSQL请求次数记录+1");
                    // 获取当前正在请求的线程数
                    activeRequestCount.incrementAndGet();
                    log.info("当前正在请求的线程数：{}", getActiveRequestCount());

                    String result;
                    // 根据模型类型调用不同的 API
                    if ("fastgpt".equalsIgnoreCase(currentModel)) {
                        log.info("使用 FastGPT API 处理段落 {}", index);
                        result = callFastGPTApi(text, apiBaseUrl, API_KEY);
                    } else {
                        log.info("使用 Gemini API 处理段落 {}", index);
                        String fullGeminiUrl = apiBaseUrl + ":generateContent"; // Gemini 需要添加端点
                        result = callGeminiApi(text, fullGeminiUrl, API_KEY);
                    }
                    log.info("段落 {} 处理完成，长度：{}", index, result.length());
                    //MYSQL线程数记录-1
                    dbFieldUpdater.updateField("api_token", "alive_thread", -1, "token", API_KEY);
                    log.info("MYSQL线程数记录-1");
                    // 获取当前正在请求的线程数
                    activeRequestCount.decrementAndGet();
                    log.info("当前正在请求的线程数:", getActiveRequestCount());
                    // 保存结果
                    String tempFileName = tempDir + "/segment_" + index + ".txt";
                    Files.write(Paths.get(tempFileName), result.getBytes());
                    log.info("段落 {} 处理完成，写入临时文件：{}", index, tempFileName);

                    // 更新进度
                    int currentCompleted = completedCount.incrementAndGet();
                    parsingProgressService.updateProgress(jobId, currentCompleted, segments.size());
                    log.info("任务ID: {} - 进度: {}/{}", jobId, currentCompleted, segments.size());

                    return tempFileName;
                } catch (Exception e) {
                    log.error("段落 {} 处理失败：{}", index, e.getMessage());
                    // Instead of returning null, rethrow the exception to be caught by the Future
                    throw new RuntimeException(e);
                }
            });

            futures.add(future);
            // ✅ 每次提交任务后等待 1 秒再继续，避免瞬间提交过多任务
            try {
                Thread.sleep(5000);  // 1000 毫秒 = 1 秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("线程休眠被中断: {}", e.getMessage());
            }
        }

        // 等待所有任务完成并收集结果
        List<String> tempFiles = new ArrayList<>();
        try {
            for (int i = 0; i < futures.size(); i++) {
                Future<String> f = futures.get(i);
                String tempFile = f.get(); // This will throw an exception if the task failed
                if (tempFile != null) {
                    tempFiles.add(tempFile);
                }
            }
        } catch (Exception e) {
            log.error("❌ 一个或多个并发任务失败，正在中止所有任务...", e);
            // On the first failure, attempt to cancel all other running tasks
            for (Future<String> f : futures) {
                f.cancel(true);
            }
            // Rethrow the exception to be caught by the top-level async handler
            throw new RuntimeException("并发处理失败", e);
        } finally {
            // Ensure the executor is always shut down
            executor.shutdownNow();
            log.info("ℹ️ 线程池已关闭");
        }

        log.info("并发处理完成，临时文件数：{}", tempFiles.size());
        return tempFiles;
    }

    /**
     * 流程3：合并临时文件，处理重复内容
     *
     * @param tempFiles  临时文件路径列表
     * @param outputFile 输出文件路径
     */
    private void mergeSegmentFiles(List<String> tempFiles, String outputFile, String chapterName) {
        log.info("开始合并临时文件：{}", tempFiles);
        try {
            // 自动创建输出目录
            Path outputPath = Paths.get(outputFile);
            Files.createDirectories(outputPath.getParent());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            // ===== 写入章节名（转换为旁白，确保进入 TTS 流程）=====
                if (chapterName != null && !chapterName.trim().isEmpty()) {
                    String chapterLine = "旁白(未知)： " + chapterName.trim();
                    writer.write(chapterLine);
                    writer.newLine();
                    log.info("章节名写入完成（旁白格式）：{}", chapterLine);
                }


                // 全局去重缓存（只比较内容，不包含章节名）
                Set<String> globalCache = new LinkedHashSet<>();

                for (int i = 0; i < tempFiles.size(); i++) {
                    String tempFile = tempFiles.get(i);
                    Path path = Paths.get(tempFile);

                    if (!Files.exists(path)) {
                        log.warn("跳过不存在的文件：{}", tempFile);
                        continue;
                    }

                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

                    // ===== 分段裁剪修正（不会少行）=====
                    List<String> linesToWrite;

                    if (i == 0) {
                        // 第一段：不裁掉头部，但裁掉尾部重叠
                        int end = Math.max(0, lines.size() - OVERLAP_KEEP);
                        linesToWrite = lines.subList(0, end);
                    } else if (i == tempFiles.size() - 1) {
                        // 最后一段：裁掉头部重叠
                        int start = Math.min(OVERLAP_KEEP, lines.size());
                        linesToWrite = lines.subList(start, lines.size());
                    } else {
                        // 中间段：去掉前后重叠
                        int start = Math.min(OVERLAP_KEEP, lines.size());
                        int end = Math.max(start, lines.size() - OVERLAP_KEEP);
                        linesToWrite = lines.subList(start, end);
                    }

                    // ===== 精准去重，不误删内容 =====
                    for (String line : linesToWrite) {

                        if (line.trim().isEmpty()) continue;

                        String normalized = normalizeLine(line);
                        if (globalCache.contains(normalized)) {
                            continue;
                        }

                        writer.write(line);
                        writer.newLine();
                        globalCache.add(normalized);

                        // 控制缓存大小
                        if (globalCache.size() > 800) {
                            Iterator<String> it = globalCache.iterator();
                            for (int x = 0; x < 300 && it.hasNext(); x++) {
                                it.next();
                                it.remove();
                            }
                        }
                    }
                }

                writer.flush();
                log.info("文件合并完成：{}", outputFile);

            } catch (Exception e) {
                log.error("合并出错：", e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * 归一化行内容，用于更准确的去重比较
     *
     * @param line 原始行内容
     * @return 归一化后的内容
     */
    private String normalizeLine(String line) {
        // 移除行首尾空白
        String normalized = line.trim();

        // 提取实际内容部分（去掉角色标注）
        // 格式：角色名(性别)：内容 或 旁白(未知)：内容
        int colonIndex = normalized.indexOf("：");
        if (colonIndex > 0) {
            // 只保留冒号后的内容用于比较
            normalized = normalized.substring(colonIndex + 1).trim();
        }

        // 统一引号（中英文引号）
        normalized = normalized.replace("\u201C", "\"").replace("\u201D", "\""); // 中文双引号 ""
        normalized = normalized.replace("\u2018", "'").replace("\u2019", "'");   // 中文单引号 ''
        normalized = normalized.replace("\"", "").replace("'", "");              // 移除所有引号

        // 移除多余空格
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    /**
     * 调用 Gemini API 进行文本转换
     *
     * @param text 输入文本
     * @return 转换后的文本
     */
    private String callGeminiApi(String text, String GEMINI_URL, String API_KEY) throws
            IOException, InterruptedException {
        // 加入提示词
        String prompt = """
                你是小说文本处理助手，所有的回答必须使用中文。 \s
                请将我提供的小说文本转换为 **角色台词与旁白分段格式**，要求如下：
                
                1. **旁白（Narration）** \s
                   所有叙述、环境描写、动作、心理活动、事件描述等非台词内容： \s
                   旁白(未知)：内容
                
                2. **角色台词（Dialogue）** \s
                   - 男性角色：角色名(男)：台词 \s
                   - 女性角色：角色名(女)：台词 \s
                   - 性别不明：角色名(未知)：台词 \s
                   - 内容必须与原文完全一致，不删改、不合并、不解释。 \s
                   - 群体对话固定标注：
                     * 宾客的议论 → 宾客(未知)
                     * 人群的惊呼 → 众人(未知)
                     * 李家弟子 → 李家弟子(男)
                     * 苏家人 → 苏家弟子(未知)
                
                3. **台词识别规则**
                   - 双引号（“”）内的文字为台词，不属于旁白。 \s
                   - 包含议论、惊呼、喊叫、对答、心理独白（以引号表示）的内容都属于台词。 \s
                   - 示例：
                     * “李玄听说是...” → 宾客(未知)
                     * “大能？” → 众人(未知)
                   - 仅纯叙述、环境描写为旁白。
                
                4. **分段要求**
                   - 每句话或自然段独立一行。 \s
                   - 每一行必须被转换成 **旁白** 或 **角色台词** 之一。 \s
                   - 心理独白若非旁白，则视为台词。 \s
                   - 保留原文全部标点、停顿、语气词、感叹号、省略号等。 \s
                   - **不能缺少行**： \s
                     原文每一行、每一句都必须保留并成功转换，不允许遗漏、合并或跳过。 \s
                
                5. **顺序与完整性**
                   - 保持原文顺序，不调整、不遗漏。 \s
                   - 不得添加任何解释或说明。 \s
                   - **不得重复输出任何已转换内容。**
                
                6. **人物判断**
                   - 根据上下文判断说话者身份。 \s
                   - 已确定性别的角色需保持一致，不得标记为“未知”。 \s
                   - 无法确定时标注为“未知”。 \s
                   - 多角色同场时，需确保每句台词归属准确。
                
                ---
                
                **输出示例：**
                - 旁白(未知)：天武大陆，东域一座小城，李家。 \s
                - 李战(男)：老祖，时辰到了。 \s
                - 旁白(未知)：李玄哼了一声，站起来，今日是他的大婚之日。 \s
                - 宾客(未知)：李家和苏家结交多年，李玄和苏婉也是天作之合。 \s
                - 宾客(未知)：是极，是极。 \s
                - 叶不凡(男)：我和婉儿才是真爱，什么李玄，不值一提！ \s
                - 苏婉(女)：没错，我和叶不凡才是真爱！ \s
                - 李家弟子(男)：家主，跟他们拼了！ \s
                - 众人(未知)：大能？
                
                ---
                
                **重要提醒：**
                - 不要将台词误判为旁白。 \s
                - 不要遗漏原文中的任何行。 \s
                - 不要重复输出相同的内容。 \s
                - 群体发言必须明确标注，不得标为“旁白”。 \s
                - 仅输出格式化文本，不要解释、总结或添加额外说明。
                """;

        String fullText = prompt + "\n\n" + text;

        // 构建请求体
        String requestBody = buildRequestBody(fullText);

        // 发送HTTP请求
        HttpResponse<String> response = sendHttpRequest(requestBody, GEMINI_URL, API_KEY);

        // 解析响应
        return parseResponse(response);
    }

    /**
     * 调用 FastGPT API 进行文本转换
     * FastGPT 接口不添加提示词，直接使用原始文本。
     *
     * @param text 输入文本
     * @return 转换后的文本
     */
    public String callFastGPTApi(String text, String FASTGPT_BASE_URL, String API_KEY) throws
            IOException, InterruptedException {
        // FastGPT API 不需要在请求中添加提示词，提示词已在FastGPT内部配置
        String fullText = text;

        // 构建 FastGPT 请求体
        String requestBody = buildFastGPTRequestBody(fullText);

        // FastGPT API 端点
        String fastGPTUrl = FASTGPT_BASE_URL + "/v1/chat/completions";

        // 发送HTTP请求 (FastGPT 使用 Bearer Token)
        HttpResponse<String> response = sendFastGPTHttpRequest(requestBody, fastGPTUrl, API_KEY);

        // 解析 FastGPT 响应
        return parseFastGPTResponse(response);
    }


    /**
     * 构建 Gemini API 请求体
     *
     * @param text 要发送的完整文本（包含提示词）
     * @return JSON 格式的请求体字符串
     */
    private String buildRequestBody(String text) {
        JSONObject part = new JSONObject();
        part.put("text", text);

        JSONArray parts = new JSONArray();
        parts.put(part);

        JSONObject contentItem = new JSONObject();
        contentItem.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(contentItem);

        JSONObject root = new JSONObject();
        root.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        JSONObject thinkingConfig = new JSONObject();
        thinkingConfig.put("thinkingBudget", THINKING_BUDGET);
        genConfig.put("thinkingConfig", thinkingConfig);
        root.put("generationConfig", genConfig);

        return root.toString();
    }

    /**
     * 构建 FastGPT 请求体（chatId 使用随机 UID）
     * FastGPT 要求严格格式：不能出现 user / customUid / variables / appId 等字段
     *
     * @param text 输入文本
     * @return JSON 请求体字符串
     */
    private String buildFastGPTRequestBody(String text) {
        // 生成随机 chatId，避免会话污染或上下文串联
        String chatId = UUID.randomUUID().toString();

        // 返回严格格式 JSON
        return """
        {
            "chatId": "%s",
            "stream": false,
            "detail": false,
            "messages": [
                {
                    "content": %s,
                    "role": "user"
                }
            ]
        }
        """.formatted(chatId, escapeJson(text));
    }


    // 简单可靠的 JSON 字符串转义
    private String escapeJson(String raw) {
        return "\"" + raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }

    /**
     * 发送 HTTP POST 请求到 Gemini API
     *
     * @param requestBody 请求体内容
     * @return HTTP 响应
     */
    private HttpResponse<String> sendHttpRequest(String requestBody, String GEMINI_URL, String API_KEY)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", API_KEY) // Gemini 使用 x-goog-api-key
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Gemini API返回状态：{}", response.statusCode());
        log.info("Gemini API返回内容：{}", response.body());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API请求失败: " + response.body());
        }

        return response;
    }

    /**
     * FastGPT 请求 —— 2025 稳定工业级版本
     * 完整解决：
     * - HTTP/1.1 header parser received no bytes
     * - EOFException
     * - 服务端断开连接
     * - 并发下连接复用失败
     */
    private HttpResponse<String> sendFastGPTHttpRequest(
            String requestBody, String FASTGPT_URL, String API_KEY
    ) throws IOException, InterruptedException {

        // 每次请求都新建 HttpClient（FastGPT 并发场景更稳定）
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)  // ❗ 强制 HTTP/1.1 — 解决你当前的核心问题
                .connectTimeout(Duration.ofSeconds(10))
                .executor(Executors.newFixedThreadPool(8)) // 防止线程被打满导致阻塞
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FASTGPT_URL))
                .timeout(Duration.ofSeconds(60)) // 单请求超时
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        int retry = 0;
        int maxRetry = 3;

        while (true) {
            try {

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                log.info("FastGPT 状态码: {}", response.statusCode());

                if (response.statusCode() == 200) {
                    return response;
                }

                // 非 200 也可能是服务端未返回 body 的异常情况
                log.warn("FastGPT 状态异常 [{}]: {}", response.statusCode(), response.body());
                throw new IOException("FastGPT HTTP " + response.statusCode());

            } catch (IOException | InterruptedException e) {

                // 💥 关键：捕获你的典型错误
                if (e.getMessage() != null &&
                        e.getMessage().contains("header parser received no bytes")) {
                    log.error("FastGPT 未返回 Header（服务端主动断开）重试中...");
                }

                retry++;
                if (retry > maxRetry) {
                    log.error("FastGPT 重试 {} 次后仍失败", maxRetry);
                    throw e;
                }

                // 指数退避，避免压垮 FastGPT
                Thread.sleep(300L * retry);
            }
        }
    }


    /**
     * 解析 Gemini API 响应
     *
     * @param response HTTP 响应对象
     * @return 提取的文本内容
     */
    private String parseResponse(HttpResponse<String> response) {
        JSONObject respJson = new JSONObject(response.body());
        JSONArray candidates = respJson.getJSONArray("candidates");
        if (candidates.length() == 0) return "";

        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        if (parts.length() == 0) return "";

        return parts.getJSONObject(0).getString("text");
    }

    /**
     * 解析 FastGPT API 响应
     *
     * @param response HTTP 响应对象
     * @return 提取的文本内容
     */
    private String parseFastGPTResponse(HttpResponse<String> response) {
        JSONObject respJson = new JSONObject(response.body());
        JSONArray choices = respJson.getJSONArray("choices");
        if (choices.length() == 0) return "";

        // 获取第一个选择的消息内容
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        return message.getString("content");
    }

    /**
     * 获取当前正在请求的线程数
     */
    public int getActiveRequestCount() {
        return activeRequestCount.get();
    }
}