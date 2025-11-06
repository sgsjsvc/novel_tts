package novel.tts.novel_tts.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Folder {
    private int id;
    private String fileName;
    private String parentDirectory;
    private String ur;
    private int status;
}
