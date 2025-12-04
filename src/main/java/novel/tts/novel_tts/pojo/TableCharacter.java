package novel.tts.novel_tts.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableCharacter {
    private Integer id;
    private String name;
    private String gender;
    private String characterName;
    private OffsetDateTime createAt;
    private String version;
}
