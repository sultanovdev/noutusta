package com.noutusta.laptoprepair.service;

import com.noutusta.laptoprepair.model.TelegramVisitRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private final RestTemplate telegramRestTemplate;
    private final boolean notifyEnabled;
    private final String botToken;
    private final String masterChatId;
    private final String apiBaseUrl;

    public TelegramNotificationService(
            @Qualifier("telegramRestTemplate") RestTemplate telegramRestTemplate,
            @Value("${app.telegram.notify.enabled:false}") boolean notifyEnabled,
            @Value("${app.telegram.bot-token:}") String botToken,
            @Value("${app.telegram.master-chat-id:}") String masterChatId,
            @Value("${app.telegram.api-base-url:https://api.telegram.org}") String apiBaseUrl
    ) {
        this.telegramRestTemplate = telegramRestTemplate;
        this.notifyEnabled = notifyEnabled;
        this.botToken = botToken;
        this.masterChatId = masterChatId;
        this.apiBaseUrl = apiBaseUrl;
    }

    public void notifyTelegramClick(TelegramVisitRequest visitRequest, HttpServletRequest servletRequest) {
        if (!notifyEnabled) {
            return;
        }
        if (botToken == null || botToken.isBlank() || masterChatId == null || masterChatId.isBlank()) {
            log.warn("event=telegram_visit_notify_skipped reason=missing_bot_config");
            return;
        }

        String messageText = buildMessageText(visitRequest, servletRequest);
        String url = buildSendMessageUrl();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", masterChatId);
        payload.put("text", messageText);
        payload.put("disable_web_page_preview", true);

        try {
            telegramRestTemplate.postForEntity(url, payload, String.class);
            log.info("event=telegram_visit_notified source={} page={}",
                    sanitize(visitRequest.source(), "unknown", 64),
                    sanitize(visitRequest.page(), "-", 300));
        } catch (RestClientException ex) {
            log.error("event=telegram_visit_notify_failed message={}", ex.getMessage(), ex);
        }
    }

    private String buildSendMessageUrl() {
        String normalizedBaseUrl = apiBaseUrl == null ? "https://api.telegram.org" : apiBaseUrl.trim();
        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + "/bot" + botToken + "/sendMessage";
    }

    private String buildMessageText(TelegramVisitRequest visitRequest, HttpServletRequest request) {
        String source = sanitize(visitRequest.source(), "telegram_link", 64);
        String page = sanitize(visitRequest.page(), "/", 300);
        String lang = sanitize(visitRequest.lang(), "-", 16);
        String ip = sanitize(resolveClientIp(request), "unknown", 80);
        String timestamp = Instant.now().toString();

        return """
                Saytdan Telegramingizni odam ko'rdi yoki yozish uchun kirdi.
                Manba: %s
                Sahifa: %s
                Til: %s
                IP: %s
                Vaqt: %s
                """.formatted(source, page, lang, ip, timestamp).trim();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String firstIp = forwarded.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }

    private String sanitize(String value, String fallback, int maxLength) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength);
    }
}
