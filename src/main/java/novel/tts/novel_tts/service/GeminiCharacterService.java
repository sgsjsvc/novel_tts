package novel.tts.novel_tts.service;

import novel.tts.novel_tts.pojo.*;

import java.util.List;

public interface GeminiCharacterService {
    List<TtsCharacter> getCharacters(String search, String gender, String version);

    Statistics  getCharacterStatistics();

    int importCharacters(ModelRequest modelRequest);

    List<NovelTable> selectCharacters(String novelname);

    List<TableCharacter> selectAllCharacters(String novelname);
}
