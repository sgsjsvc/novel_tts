package novel.tts.novel_tts.service.ipml;

import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.NovelTtsMapper;
import novel.tts.novel_tts.pojo.Folder;
import novel.tts.novel_tts.pojo.Playlist;
import novel.tts.novel_tts.service.NovelTtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class NovelTtsServiceIpml implements NovelTtsService {


    @Autowired
    private NovelTtsMapper novelTtsMapper;

    @Value("${gemini.GeminiConcurrentProcessor.concurrent.geminiTxt:temp/output/geminiTxt/}")
    private String geminiTxtPath;


    @Override
    public List<String> getNovelList() {
        return novelTtsMapper.getNovelList();
    }

    @Override
    public List<Folder> getChapterList(String novelName) {
        return novelTtsMapper.getChapterList(novelName);
    }

    @Override
    public List<Playlist> getChapterPlaylist(String novelName, String chapterName) {
        // Construct the path to the processed text file
        Path textFilePath = Paths.get(geminiTxtPath, novelName, chapterName + ".txt");
        log.info("ℹ️ Reading playlist from: {}", textFilePath);

        if (!Files.exists(textFilePath)) {
            log.error("❌ Playlist text file not found: {}", textFilePath);
            return Collections.emptyList();
        }

        try {
            List<String> lines = Files.readAllLines(textFilePath, StandardCharsets.UTF_8);
            List<Playlist> playlist = new ArrayList<>();
            int audioFileIndex = 1;

            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String audioFileName = String.format("%03d.wav", audioFileIndex++);
                String audioUrl = String.format("/api/audio/%s/%s/%s", novelName, chapterName, audioFileName);

                playlist.add(new Playlist(line, audioUrl));
            }
            log.info("✅ Successfully generated playlist for {}/{} with {} items.", novelName, chapterName, playlist.size());
            return playlist;

        } catch (IOException e) {
            log.error("❌ Failed to read playlist file {}: {}", textFilePath, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
