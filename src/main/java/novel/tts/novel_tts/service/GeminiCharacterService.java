package novel.tts.novel_tts.service;

import novel.tts.novel_tts.pojo.ModelRequest;
import novel.tts.novel_tts.pojo.TtsCharacter;
import novel.tts.novel_tts.pojo.Statistics;

import java.util.List;

public interface GeminiCharacterService {
    List<TtsCharacter> getCharacters(String search, String gender, String version);

    Statistics  getCharacterStatistics();

    int importCharacters(ModelRequest modelRequest);

}
