package novel.tts.novel_tts.service;

import novel.tts.novel_tts.pojo.Folder;

import java.util.List;

public interface NovelTtsService {
    List<String> getNovelList();

    List<Folder> getChapterList(String novelName);
}
