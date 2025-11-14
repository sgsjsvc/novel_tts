package novel.tts.novel_tts.util;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 章节解析进度跟踪器
 */
@Component
public class ParseProgressTracker {
    
    /**
     * 进度信息类
     */
    @Data
    public static class ProgressInfo {
        private String novelName;
        private String chapterName;
        private int totalSegments;  // 总段落数
        private AtomicInteger completedSegments = new AtomicInteger(0);  // 已完成段落数
        private String currentStep;  // 当前步骤
        private boolean completed = false;  // 是否完成
        private boolean error = false;  // 是否出错
        private String errorMessage;  // 错误信息
        private int status;  // 处理状态（完成百分比）
        
        public ProgressInfo(String novelName, String chapterName) {
            this.novelName = novelName;
            this.chapterName = chapterName;
            this.currentStep = "初始化...";
            this.status = 0;
        }
        
        /**
         * 更新处理状态（完成百分比）
         */
        public void updateStatus() {
            if (totalSegments <= 0) {
                this.status = 0;
                return;
            }
            
            // 即使标记为完成，也要根据实际完成的段落数计算百分比
            // 只有当所有段落都完成时才返回100%
            int percentage = (int) (((double) completedSegments.get() / totalSegments) * 100);
            
            // 确保百分比在0-100范围内
            percentage = Math.max(0, Math.min(100, percentage));
            
            // 如果已完成所有段落，则设置为100%
            if (completed && completedSegments.get() >= totalSegments) {
                this.status = 100;
                return;
            }
            
            this.status = percentage;
        }
        

    }
    
    // 存储各章节的进度信息
    private final ConcurrentHashMap<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();
    
    /**
     * 初始化进度跟踪
     */
    public void initProgress(String novelName, String chapterName, int totalSegments) {
        String key = novelName + "/" + chapterName;
        ProgressInfo progressInfo = new ProgressInfo(novelName, chapterName);
        progressInfo.setTotalSegments(totalSegments);
        progressInfo.setCurrentStep("开始处理...");
        progressMap.put(key, progressInfo);
    }
    
    /**
     * 更新进度
     */
    public void updateProgress(String novelName, String chapterName, String step) {
        String key = novelName + "/" + chapterName;
        ProgressInfo progressInfo = progressMap.get(key);
        if (progressInfo != null) {
            progressInfo.setCurrentStep(step);
        }
    }
    
    /**
     * 增加已完成的段落数
     */
    public void incrementCompletedSegments(String novelName, String chapterName) {
        String key = novelName + "/" + chapterName;
        ProgressInfo progressInfo = progressMap.get(key);
        if (progressInfo != null) {
            progressInfo.getCompletedSegments().incrementAndGet();
            progressInfo.setCurrentStep("处理段落中... (" + progressInfo.getCompletedSegments().get() + "/" + progressInfo.getTotalSegments() + ")");
            // 更新状态
            progressInfo.updateStatus();
        }
    }
    
    /**
     * 标记处理完成
     */
    public void markCompleted(String novelName, String chapterName) {
        String key = novelName + "/" + chapterName;
        ProgressInfo progressInfo = progressMap.get(key);
        if (progressInfo != null) {
            progressInfo.setCompleted(true);
            progressInfo.setCurrentStep("处理完成");
            // 更新状态
            progressInfo.updateStatus();
        }
    }
    
    /**
     * 标记处理出错
     */
    public void markError(String novelName, String chapterName, String errorMessage) {
        String key = novelName + "/" + chapterName;
        ProgressInfo progressInfo = progressMap.get(key);
        if (progressInfo != null) {
            progressInfo.setError(true);
            progressInfo.setErrorMessage(errorMessage);
            progressInfo.setCurrentStep("处理出错: " + errorMessage);
            // 出错时将状态设为0
            progressInfo.setStatus(0);
        }
    }
    
    /**
     * 获取进度信息
     */
    public ProgressInfo getProgress(String novelName, String chapterName) {
        String key = novelName + "/" + chapterName;
        return progressMap.get(key);
    }

}