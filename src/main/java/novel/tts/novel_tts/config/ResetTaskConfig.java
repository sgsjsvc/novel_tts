package novel.tts.novel_tts.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "reset")
public class ResetTaskConfig {

    /** 是否启用该功能 */
    private boolean enabled = false;

    /** 重置任务列表 */
    private List<Task> tasks;

    @Data
    public static class Task {
        private String table;      // 表名
        private String field;      // 字段名
        private Object value;      // 要重置的值
        private String condition;  // 可选条件（如 WHERE status='active'）
    }
}
