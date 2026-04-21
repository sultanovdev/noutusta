package com.noutusta.laptoprepair.service;

import com.noutusta.laptoprepair.model.ChatMessageLog;
import com.noutusta.laptoprepair.model.ChatResponse;
import com.noutusta.laptoprepair.repository.ChatLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ChatService {

    private final AiClient aiClient;
    private final ChatLogRepository chatLogRepository;

    public ChatService(AiClient aiClient, ChatLogRepository chatLogRepository) {
        this.aiClient = aiClient;
        this.chatLogRepository = chatLogRepository;
    }

    public ChatResponse processMessage(String userMessage) {
        String reply = aiClient.generateReply(userMessage)
                .orElseGet(() -> buildMockResponse(userMessage));

        Instant now = Instant.now();
        chatLogRepository.save(new ChatMessageLog(userMessage, reply, now));
        return new ChatResponse(reply, now);
    }

    private String buildMockResponse(String userMessage) {
        String normalized = userMessage.toLowerCase();
        if (normalized.contains("narx") || normalized.contains("price") || normalized.contains("cost")) {
            return "Diagnostika odatda 20 000 somdan boshlanadi, yakuniy narx ehtiyot qismga bogliq. "
                    + "Noutbuk modeli va muammoni yozsangiz, tezkor taxmin beramiz.";
        }
        if (normalized.contains("vaqt") || normalized.contains("time") || normalized.contains("tez")) {
            return "Aksar ta'mirlar 24-48 soatda tugaydi. Tezkor holatlarda shu kunning ozida xizmat korsatamiz.";
        }
        return "Xabaringiz uchun rahmat. Biz ekran, klaviatura, batareya, quvvatlash va plata ta'miri bilan shugullanamiz. "
                + "Aniq model va nosozlikni yozing, sizga tezda javob beramiz.";
    }
}
