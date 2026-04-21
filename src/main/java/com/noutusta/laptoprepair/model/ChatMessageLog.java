package com.noutusta.laptoprepair.model;

import java.time.Instant;

public record ChatMessageLog(
        String userMessage,
        String botReply,
        Instant timestamp
) {
}
