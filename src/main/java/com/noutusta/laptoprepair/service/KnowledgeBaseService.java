package com.noutusta.laptoprepair.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noutusta.laptoprepair.config.RagProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final RagProperties ragProperties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private volatile List<KnowledgeChunk> chunks = List.of();

    public KnowledgeBaseService(RagProperties ragProperties,
                                ResourceLoader resourceLoader,
                                ObjectMapper objectMapper) {
        this.ragProperties = ragProperties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initialize() {
        this.chunks = loadKnowledgeChunks();
        log.info("event=rag_kb_loaded enabled={} size={}", ragProperties.isEnabled(), chunks.size());
    }

    public List<KnowledgeSnippet> findRelevant(String userMessage) {
        if (!ragProperties.isEnabled() || chunks.isEmpty()) {
            return List.of();
        }

        String normalizedQuery = normalize(userMessage);
        Set<String> queryTokens = tokenize(normalizedQuery);
        if (queryTokens.isEmpty()) {
            return List.of();
        }

        return chunks.stream()
                .map(chunk -> new KnowledgeSnippet(
                        chunk.id(),
                        chunk.title(),
                        chunk.content(),
                        scoreChunk(chunk, queryTokens, normalizedQuery)
                ))
                .filter(snippet -> snippet.score() >= ragProperties.getMinScore())
                .sorted(Comparator.comparingDouble(KnowledgeSnippet::score).reversed())
                .limit(Math.max(1, ragProperties.getTopK()))
                .toList();
    }

    private List<KnowledgeChunk> loadKnowledgeChunks() {
        Resource resource = resourceLoader.getResource(ragProperties.getKnowledgePath());
        if (!resource.exists()) {
            log.warn("event=rag_kb_missing path={}", ragProperties.getKnowledgePath());
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            List<KnowledgeEntry> entries = objectMapper.readValue(inputStream, new TypeReference<>() {});
            if (entries == null || entries.isEmpty()) {
                return List.of();
            }

            return entries.stream()
                    .filter(this::isEntryValid)
                    .map(this::toChunk)
                    .toList();
        } catch (IOException ex) {
            log.error("event=rag_kb_load_failed path={} message={}",
                    ragProperties.getKnowledgePath(), ex.getMessage(), ex);
            return List.of();
        }
    }

    private boolean isEntryValid(KnowledgeEntry entry) {
        return entry != null
                && entry.id() != null && !entry.id().isBlank()
                && entry.title() != null && !entry.title().isBlank()
                && entry.content() != null && !entry.content().isBlank();
    }

    private KnowledgeChunk toChunk(KnowledgeEntry entry) {
        String normalized = normalize(entry.title() + " " + entry.content() + " " + String.join(" ", safeTags(entry.tags())));
        return new KnowledgeChunk(
                entry.id().trim(),
                entry.title().trim(),
                entry.content().trim(),
                normalized,
                tokenize(normalized)
        );
    }

    private double scoreChunk(KnowledgeChunk chunk, Set<String> queryTokens, String normalizedQuery) {
        long overlap = queryTokens.stream()
                .filter(chunk.tokens()::contains)
                .count();
        if (overlap == 0 && !chunk.normalized().contains(normalizedQuery)) {
            return 0.0;
        }

        double overlapRatio = overlap / (double) queryTokens.size();
        double phraseBoost = normalizedQuery.length() >= 6 && chunk.normalized().contains(normalizedQuery) ? 0.25 : 0.0;
        return overlapRatio + phraseBoost;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(SPLIT_PATTERN.split(text))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace('’', '\'').trim();
    }

    private List<String> safeTags(List<String> tags) {
        return tags == null ? Collections.emptyList() : tags;
    }

    public record KnowledgeSnippet(String id, String title, String content, double score) {
    }

    private record KnowledgeEntry(String id, String title, String content, List<String> tags) {
    }

    private record KnowledgeChunk(String id, String title, String content, String normalized, Set<String> tokens) {
    }
}
