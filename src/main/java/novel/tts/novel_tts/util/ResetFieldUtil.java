package novel.tts.novel_tts.util;

import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.config.ResetTaskConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResetFieldUtil {

    private final JdbcTemplate jdbcTemplate;
    private final ResetTaskConfig config;

    public ResetFieldUtil(JdbcTemplate jdbcTemplate, ResetTaskConfig config) {
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
    }

    /**
     * === 新增功能开始：执行所有配置的重置任务 ===
     */
    public void executeResetTasks() {
        if (!config.isEnabled()) {
            log.info("\uD83D\uDEAB [INIT] 字段重置功能已禁用 (reset.enabled=false)");
            return;
        }

        if (config.getTasks() == null || config.getTasks().isEmpty()) {
            log.info("❌ 未检测到任何字段重置任务");
            return;
        }

        for (ResetTaskConfig.Task task : config.getTasks()) {
            try {
                String condition = (task.getCondition() == null || task.getCondition().isBlank())
                        ? ""
                        : " " + task.getCondition().trim();
                String sql = String.format("UPDATE %s SET %s = ?%s",
                        task.getTable(), task.getField(), condition);
                int rows = jdbcTemplate.update(sql, task.getValue());
                log.info("\uD83D\uDCA1 已重置表 [{}], 字段 [{}], 值为 [{}], 影响行数 [{}], 条件 [{}]",
                        task.getTable(), task.getField(), task.getValue(), rows, condition);
            } catch (Exception e) {
                log.error("❌ 重置任务执行失败：表={} 字段={} 错误={}", task.getTable(), task.getField(), e.getMessage());
            }
        }
    }
}
