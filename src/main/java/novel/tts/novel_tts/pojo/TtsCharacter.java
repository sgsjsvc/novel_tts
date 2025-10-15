package novel.tts.novel_tts.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsCharacter {
    private Integer id;
    private String modelname;
    private String gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String version;
}
