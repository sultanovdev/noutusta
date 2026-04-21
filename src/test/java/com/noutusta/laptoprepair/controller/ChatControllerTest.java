package com.noutusta.laptoprepair.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noutusta.laptoprepair.model.ChatResponse;
import com.noutusta.laptoprepair.service.ChatService;
import com.noutusta.laptoprepair.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void chatReturnsMockedReply() throws Exception {
        given(chatService.processMessage(anyString(), any(Locale.class)))
                .willReturn(new ChatResponse("Salom, qanday yordam bera olaman?", Instant.now(), false, null));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Payload("Assalomu alaykum"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Salom, qanday yordam bera olaman?"));
    }

    private record Payload(String message) {
    }
}
