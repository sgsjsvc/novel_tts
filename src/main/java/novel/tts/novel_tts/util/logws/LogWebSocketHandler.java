package novel.tts.novel_tts.util.logws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler：维护连接并支持广播日志消息。
 * 使用 CopyOnWriteArraySet 保证线程安全（连接变化不频繁，广播频繁时性能可接受）。
 */
@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    private static final Set<WebSocketSession> SESSIONS = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SESSIONS.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SESSIONS.remove(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 本示例不处理客户端消息；如果需要实现心跳或订阅，可扩展逻辑
    }

    /**
     * 广播文本消息到所有已连接客户端（线程安全）
     */
    public static void broadcast(String message) {
        // 附加时间戳（可按需）
        String full = LocalDateTime.now() + " " + message;
        TextMessage text = new TextMessage(full);
        for (WebSocketSession session : SESSIONS) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(text);
                }
            } catch (Exception e) {
                // 发送异常只记录，不抛出，保证广播不中断
                e.printStackTrace();
            }
        }
    }
}
