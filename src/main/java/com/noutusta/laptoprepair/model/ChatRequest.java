package com.noutusta.laptoprepair.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Xabar bosh bolmasligi kerak")
        @Size(min = 2, max = 500, message = "Xabar uzunligi 2 dan 500 tagacha bolishi kerak")
        String message
) {
}
