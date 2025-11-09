package novel.tts.novel_tts.controller;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.common.Result;
import novel.tts.novel_tts.pojo.Folder;
import novel.tts.novel_tts.service.NovelTtsService;
import novel.tts.novel_tts.util.GeminiConcurrentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/novels")
@Slf4j
public class NovelTts {

    @Autowired
    private NovelTtsService novelTtsService;
    @Autowired
    private GeminiConcurrentProcessor geminiProcessor;

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
        List<Folder> chapterList = novelTtsService.getChapterList(novelName);
        log.info("\uD83E\uDDFE 成功获取小说:{} 的章节列表:{}", novelName, chapterList);
        return Result.success(chapterList);
    }

    @PostMapping("/{novelName}/chapters/{chapterName}/parse")
    public Result<String> parseChapter(@PathVariable String novelName, @PathVariable String chapterName, @RequestParam(defaultValue = "gemini-2.5-flash") String model) {
        log.info("▶️ 开始解析小说:{} 的章节:{}", novelName, chapterName);
        String url = novelName + "/" + chapterName;
        log.info(url);
        try {
            geminiProcessor.process(url, url, model);
        } catch (Exception e) {
            return Result.error("解析失败");
        }
        log.info("\uD83E\uDDFE 成功解析小说:{} 的章节:{}", novelName, chapterName);
        return Result.success();
    }

}
