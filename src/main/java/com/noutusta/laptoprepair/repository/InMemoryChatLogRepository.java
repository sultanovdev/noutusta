package com.noutusta.laptoprepair.repository;

import com.noutusta.laptoprepair.model.ChatMessageLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;

@Repository
public class InMemoryChatLogRepository implements ChatLogRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryChatLogRepository.class);

    private final int maxSize;
    // FIX E: CopyOnWriteArrayList → bounded ArrayDeque (FIFO eviction)
    private final Deque<ChatMessageLog> logs;
    private final Object lock = new Object();

    public InMemoryChatLogRepository(
            @Value("${app.chat-log.max-size:1000}") int maxSize
    ) {
        this.maxSize = maxSize;
        this.logs = new ArrayDeque<>(maxSize + 1);
    }

    @Override
    public void save(ChatMessageLog messageLog) {
        synchronized (lock) {
            if (logs.size() >= maxSize) {
                ChatMessageLog evicted = logs.pollFirst(); // eng eski o'chadi
                log.debug("Chat log evicted (oldest): {}", evicted != null ? evicted.timestamp() : "null");
            }
            logs.addLast(messageLog);
        }
        log.debug("Chat log persisted in memory at {}", messageLog.timestamp());
    }
}