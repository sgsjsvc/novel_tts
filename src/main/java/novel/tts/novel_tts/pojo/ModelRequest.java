package novel.tts.novel_tts.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelRequest {
    private String apiUrl;
    private String version;
}
