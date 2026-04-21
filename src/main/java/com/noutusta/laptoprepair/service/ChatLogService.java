package com.noutusta.laptoprepair.service;

import com.noutusta.laptoprepair.config.ChatLogProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class ChatLogService {

    private final int maxSize;
    private final Deque<String> logs = new ConcurrentLinkedDeque<>();

    public ChatLogService(ChatLogProperties props) {
        this.maxSize = props.getMaxSize();
    }

    public void add(String message) {
        logs.addLast(message);

        while (logs.size() > maxSize) {
            logs.pollFirst();
        }
    }

    public List<String> getAll() {
        return new ArrayList<>(logs);
    }
}
