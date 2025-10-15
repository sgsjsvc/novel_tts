package novel.tts.novel_tts.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Statistics {
    private int total;
    private Map<String, Integer> gender;  // 例如 {"男":3,"女":2,"未知":0}
}
