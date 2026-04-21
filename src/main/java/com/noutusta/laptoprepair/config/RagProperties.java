package com.noutusta.laptoprepair.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.rag")
@Getter
@Setter
public class RagProperties {

    private boolean enabled = true;
    private String knowledgePath = "classpath:rag/knowledge-base.json";
    private int topK = 3;
    private double minScore = 0.18;
}
