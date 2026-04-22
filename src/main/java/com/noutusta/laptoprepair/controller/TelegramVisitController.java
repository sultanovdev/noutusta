package com.noutusta.laptoprepair.controller;

import com.noutusta.laptoprepair.model.TelegramVisitRequest;
import com.noutusta.laptoprepair.service.TelegramNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telegram")
public class TelegramVisitController {

    private final TelegramNotificationService telegramNotificationService;

    public TelegramVisitController(TelegramNotificationService telegramNotificationService) {
        this.telegramNotificationService = telegramNotificationService;
    }

    @PostMapping("/visit")
    public ResponseEntity<Void> trackVisit(@Valid @RequestBody TelegramVisitRequest request,
                                           HttpServletRequest servletRequest) {
        telegramNotificationService.notifyTelegramClick(request, servletRequest);
        return ResponseEntity.accepted().build();
    }
}
