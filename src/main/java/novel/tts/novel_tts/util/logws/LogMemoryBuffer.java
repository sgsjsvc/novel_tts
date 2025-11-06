package novel.tts.novel_tts.util.logws;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 简易内存环形缓冲区，保存最近 N 条日志，供前端断线重连时一次性拉取。
 */
public class LogMemoryBuffer {

    private static final int DEFAULT_CAPACITY = 200;
    private final Deque<String> deque;
    private final int capacity;

    public LogMemoryBuffer() {
        this(DEFAULT_CAPACITY);
    }

    public LogMemoryBuffer(int capacity) {
        this.capacity = capacity;
        this.deque = new LinkedList<>();
    }

    public synchronized void push(String msg) {
        deque.addLast(msg);
        if (deque.size() > capacity) {
            deque.removeFirst();
        }
    }

    public synchronized List<String> snapshot() {
        return deque.stream().collect(Collectors.toList());
    }

    // 单例使用（简化）
    private static final LogMemoryBuffer INSTANCE = new LogMemoryBuffer(100);

    public static LogMemoryBuffer instance() {
        return INSTANCE;
    }
}
