package novel.tts.novel_tts.task;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.util.ResetFieldUtil;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupResetListener {

    private final ResetFieldUtil resetFieldUtil;

    public StartupResetListener(ResetFieldUtil resetFieldUtil) {
        this.resetFieldUtil = resetFieldUtil;
    }

    /**
     * === 新增功能：在 Spring Boot 启动完成后自动执行数据库字段重置 ===
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("⏳ [INIT] Spring Boot 启动完成，开始执行字段重置任务...");
        resetFieldUtil.executeResetTasks();
        log.info("✅ [INIT] 所有字段重置任务执行完毕。");
    }
}
