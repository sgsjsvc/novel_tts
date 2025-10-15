package novel.tts.novel_tts.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Api {
    private Integer id;
    private String name;
    private String token;
    private String note;
    private double minInterval;
    private int maxConcurrency;
    private Integer disabled;


}
