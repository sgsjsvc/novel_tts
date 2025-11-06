package novel.tts.novel_tts.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dashboard {
    private Integer totalAccounts;
    private Integer activeAccounts;
    private Integer requestFrequency;
    private Integer systemThreads;
    private Integer totalThreads;
    private List<DashboardApi> apiList;
}
