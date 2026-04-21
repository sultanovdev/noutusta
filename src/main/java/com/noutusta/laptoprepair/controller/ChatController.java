package com.noutusta.laptoprepair.controller;

import com.noutusta.laptoprepair.model.ChatRequest;
import com.noutusta.laptoprepair.model.ChatResponse;
import com.noutusta.laptoprepair.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, Locale locale) {
        log.info("event=chat_request length={} locale={}", request.message().length(), locale.getLanguage());
        ChatResponse response = chatService.processMessage(request.message(), locale);
        return ResponseEntity.ok(response);
    }
}
