package novel.tts.novel_tts.service;

import novel.tts.novel_tts.pojo.ParsingProgress;

public interface ParsingProgressService {
    ParsingProgress startNewTask();
    void updateProgress(String jobId, int completedTasks, int totalTasks);
    void completeTask(String jobId);
    void failTask(String jobId, String message);
    ParsingProgress getProgress(String jobId);
    void updateStage(String jobId, String stage);
}
