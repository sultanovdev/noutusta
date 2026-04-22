package com.noutusta.laptoprepair.service;

import com.noutusta.laptoprepair.model.TelegramVisitRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationServiceTest {

    @Mock
    private RestTemplate telegramRestTemplate;

    @Mock
    private HttpServletRequest servletRequest;

    @Test
    void skipsNotificationWhenFeatureDisabled() {
        TelegramNotificationService service = new TelegramNotificationService(
                telegramRestTemplate,
                false,
                "123456:token",
                "10203040",
                "https://api.telegram.org"
        );

        TelegramVisitRequest visitRequest = new TelegramVisitRequest(
                "sticky_cta",
                "https://t.me/begaliyev1299",
                "/",
                "",
                "uz"
        );

        service.notifyTelegramClick(visitRequest, servletRequest);
        verifyNoInteractions(telegramRestTemplate);
    }

    @Test
    void sendsFormattedMessageWhenConfigured() {
        TelegramNotificationService service = new TelegramNotificationService(
                telegramRestTemplate,
                true,
                "123456:token",
                "10203040",
                "https://api.telegram.org"
        );

        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.2");
        lenient().when(servletRequest.getRemoteAddr()).thenReturn("10.0.0.2");        when(servletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        TelegramVisitRequest visitRequest = new TelegramVisitRequest(
                "contact_details",
                "https://t.me/begaliyev1299",
                "/contact",
                "https://google.com",
                "uz"
        );

        service.notifyTelegramClick(visitRequest, servletRequest);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        verify(telegramRestTemplate).postForEntity(
                urlCaptor.capture(),
                requestCaptor.capture(),
                eq(String.class)      // ← matcher bilan o'rab
        );
        assertThat(urlCaptor.getValue()).isEqualTo("https://api.telegram.org/bot123456:token/sendMessage");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) requestCaptor.getValue();
        assertThat(payload.get("chat_id")).isEqualTo("10203040");
        assertThat(payload.get("disable_web_page_preview")).isEqualTo(true);
        assertThat(payload.get("text").toString()).contains("Source: contact_details");
        assertThat(payload.get("text").toString()).contains("IP: 203.0.113.10");
    }
}
