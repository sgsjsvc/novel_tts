package novel.tts.novel_tts.controller;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.common.Result;
import novel.tts.novel_tts.pojo.Folder;
import novel.tts.novel_tts.service.NovelTtsService;
import novel.tts.novel_tts.util.GeminiConcurrentProcessor;
import novel.tts.novel_tts.util.InferEmotionClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import novel.tts.novel_tts.pojo.Playlist;
import novel.tts.novel_tts.pojo.ParsingProgress;
import novel.tts.novel_tts.service.ParsingProgressService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/novels")
@Slf4j
@CrossOrigin(origins = "*")
public class NovelTts {

    @Value("${folder.watch.outputFlowPath:temp/output/txt/}")
    private String outputFlowPath;
    @Autowired
    private NovelTtsService novelTtsService;
    @Autowired
    private GeminiConcurrentProcessor geminiProcessor;
    @Autowired
    private ParsingProgressService parsingProgressService;

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
        log.info("▶️ 异步开始解析小说:{} 的章节:{}", novelName, chapterName);
        String url = novelName + "/" + chapterName;
        log.info(url);

        ParsingProgress progress = parsingProgressService.startNewTask();

        CompletableFuture.runAsync(() -> {
            try {
                geminiProcessor.process(url, url, model, modelVersion, progress.getJobId());
            } catch (Exception e) {
                parsingProgressService.failTask(progress.getJobId(), e.getMessage());
                log.error("❌ 解析小说:{} 的章节:{} 失败", novelName, chapterName, e);
            }
        });

        log.info("\uD83D\uDE80 成功启动小说:{} 章节:{} 的解析任务，任务ID: {}", novelName, chapterName, progress.getJobId());
        return Result.success(progress.getJobId());
    }

    @GetMapping("/parse/progress/{jobId}")
    public Result<ParsingProgress> getParseProgress(@PathVariable String jobId) {
        ParsingProgress progress = parsingProgressService.getProgress(jobId);
        if (progress == null) {
            return Result.error("未找到任务ID为: " + jobId + " 的解析任务");
        }
        return Result.success(progress);
    }

    @GetMapping("/{novelName}/chapters/{chapterName}/playlist")
    public Result<List<Playlist>> getChapterPlaylist(@PathVariable String novelName, @PathVariable String chapterName) {
        log.info("▶️ 获取小说:{} 章节:{} 的播放列表", novelName, chapterName);
        List<Playlist> playlist = novelTtsService.getChapterPlaylist(novelName, chapterName);
        log.info("\uD83C\uDFB5 成功获取播放列表，包含 {} 个项目", playlist.size());
        return Result.success(playlist);
    }






}
