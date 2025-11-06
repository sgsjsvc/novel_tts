package novel.tts.novel_tts.controller;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.common.Result;
import novel.tts.novel_tts.pojo.Folder;
import novel.tts.novel_tts.service.NovelTtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/novels")
@Slf4j
public class NovelTts {

    @Autowired
    private NovelTtsService novelTtsService;


    @GetMapping( "")
    public Result<List<String>>  getNovelList() {
        log.info("▶️ 开始获取小说列表");
        List<String> novelList = novelTtsService.getNovelList();
        log.info("\uD83E\uDDFE 成功获取小说列表:{}",novelList);
        return Result.success(novelList);
    }

    @GetMapping( "/{novelName}/chapters")
    public Result<List<Folder>> getChapterList(@PathVariable String novelName) {
        log.info("▶️ 获取小说:{} 的章节列表",novelName);
        List<Folder> chapterList = novelTtsService.getChapterList(novelName);
        log.info("\uD83E\uDDFE 成功获取小说:{} 的章节列表:{}",novelName,chapterList);
        return Result.success(chapterList);
    }

}
