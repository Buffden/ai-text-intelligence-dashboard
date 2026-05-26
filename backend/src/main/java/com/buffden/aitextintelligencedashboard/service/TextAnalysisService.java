package com.buffden.aitextintelligencedashboard.service;

import com.buffden.aitextintelligencedashboard.dto.AnalysisResponse;
import com.buffden.aitextintelligencedashboard.dto.ClassifyResponse;
import com.buffden.aitextintelligencedashboard.dto.AnalyzeRequest;
import com.buffden.aitextintelligencedashboard.exception.LlmUnavailableException;
import com.buffden.aitextintelligencedashboard.exception.ParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class TextAnalysisService {
    private static final int MAX_ATTEMPTS = 2;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String systemPrompt;
    private final String classifyPrompt;

    public TextAnalysisService(ChatClient.Builder chatClientBuilder,
                                ObjectMapper objectMapper,
                                @Value("classpath:prompts/classify-system.st") Resource classifyPromptResource,
                                @Value("classpath:prompts/analyze-system.st") Resource promptResource) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.classifyPrompt = classifyPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public AnalysisResponse analyze(AnalyzeRequest request) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                long start = System.currentTimeMillis();

                ChatResponse response = chatClient.prompt()
                        .system(systemPrompt)
                        .user(request.getText())
                        .call()
                        .chatResponse();

                long latencyMs = System.currentTimeMillis() - start;
                String raw = response.getResult().getOutput().getText();
                AnalysisResponse result = parseResponse(raw);
                logUsage(response, latencyMs);
                return result;

            } catch (Exception e) {
                lastException = e;
                log.warn("LLM attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }

        if (lastException instanceof ParseException pe) {
            throw pe;
        }
        throw new LlmUnavailableException("LLM service unavailable after " + MAX_ATTEMPTS + " attempts", lastException);
    }

    private AnalysisResponse parseResponse(String raw) {
        try {
            String cleaned = raw.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }
            return objectMapper.readValue(cleaned, AnalysisResponse.class);
        } catch (Exception e) {
            throw new ParseException("Failed to parse LLM response", raw, e);
        }
    }

    public ClassifyResponse classify(AnalyzeRequest request) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                long start = System.currentTimeMillis();

                ChatResponse response = chatClient.prompt()
                            .system(classifyPrompt)
                            .user(request.getText())
                            .call()
                            .chatResponse();
                long latencyMs = System.currentTimeMillis() - start;
                String raw = response.getResult().getOutput().getText();
                ClassifyResponse result = classifyResponse(raw);
                logUsage(response, latencyMs);
                return result;



            } catch (Exception e) {
                lastException = e;
                log.warn("LLM attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        if (lastException instanceof ParseException pe) {
            throw pe;
        }
        throw new LlmUnavailableException("LLM service unavailable after " + MAX_ATTEMPTS + " attempts", lastException);
    }

    private ClassifyResponse classifyResponse(String raw) {
        try {
            String cleaned = raw.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }
            return objectMapper.readValue(cleaned, ClassifyResponse.class);
        } catch (Exception e) {
            throw new ParseException("Failed to classify LLM response", raw, e);
        }
    }

    private void logUsage(ChatResponse response, long latencyMs) {
        var usage = response.getMetadata().getUsage();
        log.info("LLM usage — model: {}, input tokens: {}, output tokens: {}, total: {}, latency: {}ms",
                response.getMetadata().getModel(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens(),
                latencyMs);
    }
}
