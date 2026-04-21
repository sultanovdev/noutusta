package com.noutusta.laptoprepair.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AiClient {

    public static final String HANDOFF_TOKEN = "__HANDOFF__";
    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final RestTemplate aiRestTemplate;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.ai.url:https://api.openai.com/v1/chat/completions}")
    private String aiUrl;

    public AiClient(RestTemplate aiRestTemplate) {
        this.aiRestTemplate = aiRestTemplate;
    }

    public Optional<String> generateReply(String userMessage) {
        return generateReplyWithContext(userMessage, List.of());
    }

    public Optional<String> generateReplyWithContext(String userMessage,
                                                     List<KnowledgeBaseService.KnowledgeSnippet> contextSnippets) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("event=ai_fallback reason=missing_api_key");
            return Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String knowledgeContext = buildKnowledgeContext(contextSnippets);
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", """
                                    You are an assistant for a laptop repair service named Noutusta.
                                    Use only the provided knowledge context.
                                    If context is not enough or the question is outside context, return exactly %s.
                                    Keep answer concise and practical in the user's language.
                                    Do not fabricate prices, timings or contacts not in context.
                                    """.formatted(HANDOFF_TOKEN)),
                            Map.of("role", "user", "content", """
                                    CUSTOMER QUESTION:
                                    %s

                                    KNOWLEDGE CONTEXT:
                                    %s
                                    """.formatted(userMessage, knowledgeContext))
                    ),
                    "temperature", 0.2
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            OpenAiResponse response = aiRestTemplate.postForObject(aiUrl, entity, OpenAiResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("event=ai_fallback reason=empty_provider_response");
                return Optional.empty();
            }

            // FIX D-1: message() null tekshiruvi
            Choice firstChoice = response.choices().getFirst();
            if (firstChoice.message() == null) {
                log.warn("event=ai_fallback reason=null_message");
                return Optional.empty();
            }

            String content = firstChoice.message().content();
            return Optional.ofNullable(content).filter(c -> !c.isBlank());

        } catch (RestClientException ex) {
            log.error("event=ai_request_failed type=rest_client message={}", ex.getMessage(), ex);
            return Optional.empty();
        } catch (RuntimeException ex) {
            // FIX D-2: NPE va boshqa kutilmagan xatolar ham ushlandi
            log.error("event=ai_request_failed type=unexpected message={}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private record OpenAiResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}

    private String buildKnowledgeContext(List<KnowledgeBaseService.KnowledgeSnippet> contextSnippets) {
        if (contextSnippets == null || contextSnippets.isEmpty()) {
            return "(no relevant context)";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contextSnippets.size(); i++) {
            KnowledgeBaseService.KnowledgeSnippet snippet = contextSnippets.get(i);
            sb.append(i + 1)
                    .append(". ")
                    .append(snippet.title())
                    .append(": ")
                    .append(snippet.content())
                    .append("\n");
        }
        return sb.toString().trim();
    }
}
