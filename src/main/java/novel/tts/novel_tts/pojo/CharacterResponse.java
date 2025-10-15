package novel.tts.novel_tts.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterResponse {
    private List<TtsCharacter> data;  // 角色列表
    private Statistics statistics;        // 统计信息
    private List<String> versions;
}

