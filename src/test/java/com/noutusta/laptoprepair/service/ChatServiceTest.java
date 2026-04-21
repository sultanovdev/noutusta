package com.noutusta.laptoprepair.service;

import com.noutusta.laptoprepair.model.ChatResponse;
import com.noutusta.laptoprepair.repository.ChatLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private ChatLogRepository chatLogRepository;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private MessageSource messageSource;

    @Test
    void returnsAiReplyWhenKnowledgeFound() {
        ChatService service = new ChatService(
                aiClient,
                chatLogRepository,
                knowledgeBaseService,
                messageSource,
                "https://t.me/begaliyev1299"
        );

        String userMessage = "Lenovo ekranida chiziq chiqyapti";
        List<KnowledgeBaseService.KnowledgeSnippet> snippets = List.of(
                new KnowledgeBaseService.KnowledgeSnippet("services-screen", "Ekran", "Ekran ta'miri bor", 0.91)
        );

        given(knowledgeBaseService.findRelevant(userMessage)).willReturn(snippets);
        given(aiClient.generateReplyWithContext(userMessage, snippets)).willReturn(Optional.of("Ekran almashtirish kerak bo'lishi mumkin."));

        ChatResponse response = service.processMessage(userMessage, Locale.forLanguageTag("uz"));

        assertThat(response.reply()).contains("Ekran almashtirish");
        assertThat(response.handoffRequired()).isFalse();
        assertThat(response.handoffUrl()).isNull();
        verify(chatLogRepository).save(any());
    }

    @Test
    void returnsTelegramHandoffWhenNoRelevantKnowledge() {
        ChatService service = new ChatService(
                aiClient,
                chatLogRepository,
                knowledgeBaseService,
                messageSource,
                "https://t.me/begaliyev1299"
        );

        String userMessage = "Menga 3D printer kerak edi";
        Locale locale = Locale.forLanguageTag("uz");

        given(knowledgeBaseService.findRelevant(userMessage)).willReturn(List.of());
        given(messageSource.getMessage(eq("chat.handoff.message"), any(Object[].class), eq(locale)))
                .willReturn("Telegramga yozing: https://t.me/begaliyev1299");

        ChatResponse response = service.processMessage(userMessage, locale);

        assertThat(response.handoffRequired()).isTrue();
        assertThat(response.handoffUrl()).isEqualTo("https://t.me/begaliyev1299");
        assertThat(response.reply()).contains("Telegramga yozing");

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(messageSource).getMessage(eq("chat.handoff.message"), argsCaptor.capture(), eq(locale));
        assertThat(argsCaptor.getValue()[0]).isEqualTo("https://t.me/begaliyev1299");
    }
}
