package novel.tts.novel_tts.pojo;

import lombok.Data;

@Data
public class ParsingProgress {
    private String jobId;
    private String status;
    private int totalTasks;
    private int completedTasks;
    private double progress;
    private String message;
    private String currentStage;
}
