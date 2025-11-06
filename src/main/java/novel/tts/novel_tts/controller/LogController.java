package novel.tts.novel_tts.controller;

import novel.tts.novel_tts.util.logws.LogMemoryBuffer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提供历史日志快照接口（断线重连或页面首次加载时拉取）。
 */
@RestController
@RequestMapping("ws/logs")
public class LogController {

    @GetMapping("/recent")
    public List<String> recent() {
        return LogMemoryBuffer.instance().snapshot();
    }
}
