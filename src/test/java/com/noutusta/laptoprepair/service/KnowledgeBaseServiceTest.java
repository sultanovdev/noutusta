package com.noutusta.laptoprepair.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noutusta.laptoprepair.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeBaseServiceTest {

    @Test
    void findsRelevantKnowledgeByQuery() {
        RagProperties props = new RagProperties();
        props.setEnabled(true);
        props.setKnowledgePath("classpath:rag/knowledge-base.json");
        props.setTopK(3);
        props.setMinScore(0.1);

        KnowledgeBaseService service = new KnowledgeBaseService(
                props,
                new DefaultResourceLoader(),
                new ObjectMapper()
        );
        service.initialize();

        var results = service.findRelevant("diagnostika narxi qancha");

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().id()).isEqualTo("pricing-diagnostics");
    }
}
