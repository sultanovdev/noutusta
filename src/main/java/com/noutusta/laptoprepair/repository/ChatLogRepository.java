package com.noutusta.laptoprepair.repository;

import com.noutusta.laptoprepair.model.ChatMessageLog;

public interface ChatLogRepository {
    void save(ChatMessageLog messageLog);
}
