package com.noutusta.laptoprepair.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noutusta.laptoprepair.model.ChatResponse;
import com.noutusta.laptoprepair.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void chatReturnsMockedReply() throws Exception {
        given(chatService.processMessage(anyString()))
                .willReturn(new ChatResponse("Salom, qanday yordam bera olaman?", Instant.now()));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Payload("Assalomu alaykum"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Salom, qanday yordam bera olaman?"));
    }

    private record Payload(String message) {
    }
}
