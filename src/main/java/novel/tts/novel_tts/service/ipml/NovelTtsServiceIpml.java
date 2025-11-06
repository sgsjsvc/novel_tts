package novel.tts.novel_tts.service.ipml;

import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.NovelTtsMapper;
import novel.tts.novel_tts.pojo.Folder;
import novel.tts.novel_tts.service.NovelTtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class NovelTtsServiceIpml implements NovelTtsService {


    @Autowired
    private NovelTtsMapper novelTtsMapper;


    @Override
    public List<String> getNovelList() {
        return novelTtsMapper.getNovelList();
    }

    @Override
    public List<Folder> getChapterList(String novelName) {

        return novelTtsMapper.getChapterList(novelName);
    }
}
