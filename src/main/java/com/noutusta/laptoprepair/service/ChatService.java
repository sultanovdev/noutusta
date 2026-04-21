package com.noutusta.laptoprepair.service;

import com.noutusta.laptoprepair.model.ChatMessageLog;
import com.noutusta.laptoprepair.model.ChatResponse;
import com.noutusta.laptoprepair.repository.ChatLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final AiClient aiClient;
    private final ChatLogRepository chatLogRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final MessageSource messageSource;
    private final String telegramUrl;

    public ChatService(AiClient aiClient,
                       ChatLogRepository chatLogRepository,
                       KnowledgeBaseService knowledgeBaseService,
                       MessageSource messageSource,
                       @Value("${app.support.telegram-url:https://t.me/begaliyev1299}") String telegramUrl) {
        this.aiClient = aiClient;
        this.chatLogRepository = chatLogRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.messageSource = messageSource;
        this.telegramUrl = telegramUrl;
    }

    public ChatResponse processMessage(String userMessage, Locale locale) {
        List<KnowledgeBaseService.KnowledgeSnippet> relevantKnowledge = knowledgeBaseService.findRelevant(userMessage);
        log.info("event=chat_rag_lookup match_count={} top_score={}",
                relevantKnowledge.size(),
                relevantKnowledge.isEmpty() ? 0.0 : relevantKnowledge.getFirst().score());

        boolean handoffRequired = false;
        String handoffUrl = null;
        String reply;

        if (relevantKnowledge.isEmpty()) {
            log.info("event=chat_handoff reason=no_relevant_context");
            handoffRequired = true;
            handoffUrl = telegramUrl;
            reply = handoffMessage(locale);
        } else {
            String aiReply = aiClient.generateReplyWithContext(userMessage, relevantKnowledge).orElse(null);
            if (aiReply == null || aiReply.isBlank()) {
                log.info("event=chat_ai_fallback reason=empty_ai_reply");
                reply = relevantKnowledge.getFirst().content();
            } else if (requiresHandoff(aiReply)) {
                log.info("event=chat_handoff reason=ai_escalation_marker");
                handoffRequired = true;
                handoffUrl = telegramUrl;
                reply = handoffMessage(locale);
            } else {
                reply = aiReply.trim();
            }
        }

        Instant now = Instant.now();
        chatLogRepository.save(new ChatMessageLog(userMessage, reply, now));
        return new ChatResponse(reply, now, handoffRequired, handoffUrl);
    }

    private boolean requiresHandoff(String aiReply) {
        String normalized = aiReply.toLowerCase(Locale.ROOT);
        return aiReply.contains(AiClient.HANDOFF_TOKEN)
                || normalized.contains("bilmayman")
                || normalized.contains("aniq malumot yoq")
                || normalized.contains("ma'lumot yo'q")
                || normalized.contains("i don't know")
                || normalized.contains("not enough context")
                || normalized.contains("insufficient context");
    }

    private String handoffMessage(Locale locale) {
        return messageSource.getMessage("chat.handoff.message", new Object[]{telegramUrl}, locale);
    }
}
