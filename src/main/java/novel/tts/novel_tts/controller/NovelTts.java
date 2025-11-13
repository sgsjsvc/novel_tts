package novel.tts.novel_tts.controller;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.common.Result;
import novel.tts.novel_tts.pojo.Folder;
import novel.tts.novel_tts.service.NovelTtsService;
import novel.tts.novel_tts.util.GeminiConcurrentProcessor;
import novel.tts.novel_tts.util.InferEmotionClient;
import novel.tts.novel_tts.util.ParseProgressTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/novels")
@Slf4j
public class NovelTts {

    @Value("${folder.watch.outputFlowPath:temp/output/txt/}")
    private String outputFlowPath;
    @Autowired
    private NovelTtsService novelTtsService;
    @Autowired
    private GeminiConcurrentProcessor geminiProcessor;
    @Autowired
    private ParseProgressTracker progressTracker;

    @GetMapping("")
    public Result<List<String>> getNovelList() {
        log.info("▶️ 开始获取小说列表");
        List<String> novelList = novelTtsService.getNovelList();
        log.info("\uD83E\uDDFE 成功获取小说列表:{}", novelList);
        return Result.success(novelList);
    }

    @GetMapping("/{novelName}/chapters")
    public Result<List<Folder>> getChapterList(@PathVariable String novelName) {
        log.info("▶️ 获取小说:{} 的章节列表", novelName);
        String novel=outputFlowPath+novelName;
        log.info("✅ 小说url：{}", novel);
        List<Folder> chapterList = novelTtsService.getChapterList(novel);
        log.info("\uD83E\uDDFE 成功获取小说:{} 的章节列表:{}", novelName, chapterList);
        return Result.success(chapterList);
    }

    @PostMapping("/{novelName}/chapters/{chapterName}/parse")
    public Result<String> parseChapter(@PathVariable String novelName, @PathVariable String chapterName, @RequestParam(defaultValue = "gemini-2.5-flash") String model, @RequestParam(defaultValue = "v2") String modelVersion) {
        log.info("▶️ 开始解析小说:{} 的章节:{}", novelName, chapterName);
        log.info("✅ model：{}", model);
        String url = novelName + "/" + chapterName;
        log.info(url);

        // 异步处理任务
        new Thread(() -> {
            try {
                geminiProcessor.process(url, url, model, modelVersion);
            } catch (Exception e) {
                log.error("解析小说:{} 的章节:{} 失败", novelName, chapterName, e);
                // 标记处理出错
                progressTracker.markError(novelName, chapterName, e.getMessage());
            }
        }).start();

        return Result.success("开始解析任务");
    }

    @GetMapping("/{novelName}/chapters/{chapterName}/progress")
    public Result<ParseProgressTracker.ProgressInfo> getParseProgress(@PathVariable String novelName, @PathVariable String chapterName) {
        log.info("▶️ 获取小说:{} 的章节:{} 解析进度", novelName, chapterName);
        ParseProgressTracker.ProgressInfo progressInfo = progressTracker.getProgress(novelName, chapterName);
        if (progressInfo == null) {
            return Result.error("未找到解析进度信息");
        }
        log.info("\uD83E\uDDFE 获取小说:{} 的章节:{} 解析进度:{}", novelName, chapterName, progressInfo);
        return Result.success(progressInfo);
    }

}