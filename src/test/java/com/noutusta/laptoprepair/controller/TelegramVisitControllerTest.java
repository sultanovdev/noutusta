package com.noutusta.laptoprepair.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noutusta.laptoprepair.service.RateLimiterService;
import com.noutusta.laptoprepair.service.TelegramNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelegramVisitController.class)
@AutoConfigureMockMvc(addFilters = false)
class TelegramVisitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TelegramNotificationService telegramNotificationService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void returnsAcceptedAndForwardsPayloadToService() throws Exception {
        Payload payload = new Payload(
                "sticky_cta",
                "https://t.me/begaliyev1299",
                "/contact",
                "https://google.com",
                "uz"
        );

        mockMvc.perform(post("/api/telegram/visit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted());

        verify(telegramNotificationService).notifyTelegramClick(any(), any());
    }

    @Test
    void returnsBadRequestWhenPayloadIsInvalid() throws Exception {
        Payload payload = new Payload(
                "",
                "",
                "/contact",
                "https://google.com",
                "uz"
        );

        mockMvc.perform(post("/api/telegram/visit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    private record Payload(String source, String target, String page, String referrer, String lang) {
    }
}
