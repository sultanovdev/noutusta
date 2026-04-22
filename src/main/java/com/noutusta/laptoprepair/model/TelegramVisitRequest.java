package com.noutusta.laptoprepair.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TelegramVisitRequest(
        @NotBlank(message = "Manba kiritilishi kerak")
        @Size(max = 64, message = "Manba 64 belgidan oshmasligi kerak")
        String source,

        @NotBlank(message = "Telegram linki bo'sh bo'lmasligi kerak")
        @Size(max = 400, message = "Telegram linki juda uzun")
        String target,

        @Size(max = 300, message = "Sahifa qiymati juda uzun")
        String page,

        @Size(max = 400, message = "Referrer qiymati juda uzun")
        String referrer,

        @Size(max = 16, message = "Til kodi juda uzun")
        String lang
) {
}
