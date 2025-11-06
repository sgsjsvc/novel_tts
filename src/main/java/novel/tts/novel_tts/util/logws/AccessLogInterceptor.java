package novel.tts.novel_tts.util.logws;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
@Slf4j
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 打印访问日志
        log.info("⏰ 文件访问时间: " + LocalDateTime.now()
                + ", \uD83C\uDF10 IP: " + request.getRemoteAddr()
                + ", \uD83E\uDDED 访问URL: " + request.getRequestURI());
        return true; // 放行请求
    }
}
