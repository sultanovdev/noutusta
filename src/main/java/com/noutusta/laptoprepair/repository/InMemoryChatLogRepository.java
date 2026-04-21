package com.noutusta.laptoprepair.repository;

import com.noutusta.laptoprepair.model.ChatMessageLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class InMemoryChatLogRepository implements ChatLogRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryChatLogRepository.class);
    private final List<ChatMessageLog> logs = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void save(ChatMessageLog messageLog) {
        logs.add(messageLog);
        log.debug("Chat log persisted in memory at {}", messageLog.timestamp());
    }
}
