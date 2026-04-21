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
        if (apiKey == null || apiKey.isBlank()) {
            log.info("event=ai_fallback reason=missing_api_key");
            return Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a helpful laptop repair assistant."),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.3
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
}