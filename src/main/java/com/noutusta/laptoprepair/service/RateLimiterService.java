package com.noutusta.laptoprepair.service;

import com.noutusta.laptoprepair.config.RateLimitProperties;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class RateLimiterService {

    private final RateLimitProperties props;

    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimitProperties props) {
        this.props = props;
    }

    public boolean allow(String key) {
        long now = System.currentTimeMillis();

        requests.putIfAbsent(key, new ConcurrentLinkedDeque<>());
        Deque<Long> timestamps = requests.get(key);

        synchronized (timestamps) {
            while (!timestamps.isEmpty() &&
                    now - timestamps.peekFirst() > props.getWindowSeconds() * 1000L) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= props.getMaxRequests()) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }
}