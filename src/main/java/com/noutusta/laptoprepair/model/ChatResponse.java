package com.noutusta.laptoprepair.model;

import java.time.Instant;

public record ChatResponse(
        String reply,
        Instant timestamp,
        boolean handoffRequired,
        String handoffUrl
) {
}
