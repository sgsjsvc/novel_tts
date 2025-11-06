package novel.tts.novel_tts.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardApi {
    private String name;
    private int maxConcurrency;
    private Integer aliveThread;
}
