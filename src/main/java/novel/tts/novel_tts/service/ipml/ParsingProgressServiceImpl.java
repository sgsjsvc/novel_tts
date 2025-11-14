package novel.tts.novel_tts.service.ipml;

import novel.tts.novel_tts.pojo.ParsingProgress;
import novel.tts.novel_tts.service.ParsingProgressService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ParsingProgressServiceImpl implements ParsingProgressService {

    private final ConcurrentMap<String, ParsingProgress> progressMap = new ConcurrentHashMap<>();

    @Override
    public ParsingProgress startNewTask() {
        String jobId = UUID.randomUUID().toString();
        ParsingProgress progress = new ParsingProgress();
        progress.setJobId(jobId);
        progress.setStatus("RUNNING");
        progress.setTotalTasks(0);
        progress.setCompletedTasks(0);
        progress.setProgress(0.0);
        progress.setCurrentStage("PARSING"); // Set initial stage
        progressMap.put(jobId, progress);
        return progress;
    }

    @Override
    public void updateProgress(String jobId, int completedTasks, int totalTasks) {
        ParsingProgress progress = progressMap.get(jobId);
        if (progress != null) {
            progress.setCompletedTasks(completedTasks);
            progress.setTotalTasks(totalTasks);
            progress.setProgress(totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0);
        }
    }

    @Override
    public void completeTask(String jobId) {
        ParsingProgress progress = progressMap.get(jobId);
        if (progress != null) {
            progress.setStatus("COMPLETED");
            progress.setProgress(100.0);
        }
    }

    @Override
    public void failTask(String jobId, String message) {
        ParsingProgress progress = progressMap.get(jobId);
        if (progress != null) {
            progress.setStatus("FAILED");
            progress.setMessage(message);
        }
    }

    @Override
    public ParsingProgress getProgress(String jobId) {
        return progressMap.get(jobId);
    }

    @Override
    public void updateStage(String jobId, String stage) {
        ParsingProgress progress = progressMap.get(jobId);
        if (progress != null) {
            progress.setCurrentStage(stage);
            progress.setCompletedTasks(0);
            progress.setTotalTasks(0);
            progress.setProgress(0.0);
        }
    }
}
