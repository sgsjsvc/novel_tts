package novel.tts.novel_tts.util.logws;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * 自定义 Logback Appender：把日志发到 WebSocketHandler（实时推送），并写入内存缓冲区。
 *
 * 注意：
 * - 该类会在 Logback 初始化阶段被 logback-spring.xml 加载。
 * - 不要在 append 方法内做太重的同步阻塞操作，最好快速返回。
 */
public class WebSocketAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent eventObject) {
        try {
            String formatted = eventObject.getFormattedMessage();
            // 也可包含级别、logger 名称等信息
            String msg = String.format("[%s] %s - %s", eventObject.getLevel(), eventObject.getLoggerName(), formatted);

            // 将日志放入内存缓冲（用于断线重连）
            LogMemoryBuffer.instance().push(msg);

            // 通过 WebSocket 实时广播
            LogWebSocketHandler.broadcast(msg);

            // 注意：这里不要抛出异常（Appender 不应中断主流程）
        } catch (Exception e) {
            // 避免抛出异常中断日志系统
            e.printStackTrace();
        }
    }
}
