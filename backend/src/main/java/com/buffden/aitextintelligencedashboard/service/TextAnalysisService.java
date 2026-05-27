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
import org.springframework.ai.openai.OpenAiChatOptions;
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
    private final String fallbackModel;

    public TextAnalysisService(ChatClient.Builder chatClientBuilder,
                                ObjectMapper objectMapper,
                                @Value("classpath:prompts/classify-system.st") Resource classifyPromptResource,
                                @Value("classpath:prompts/analyze-system.st") Resource promptResource,
                                @Value("${app.llm.fallback-model}") String fallbackModel) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.classifyPrompt = classifyPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        this.fallbackModel = fallbackModel;
    }

    public AnalysisResponse analyze(AnalyzeRequest request) {
        try {
            return attemptAnalyze(request, null);
        } catch (LlmUnavailableException e) {
            log.warn("Primary model exhausted, routing to fallback: {}", fallbackModel);
            return attemptAnalyze(request, fallbackModel);
        }
    }

    private AnalysisResponse attemptAnalyze(AnalyzeRequest request, String modelOverride) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                long start = System.currentTimeMillis();

                var spec = chatClient.prompt()
                        .system(systemPrompt)
                        .user("<text>" + request.getText() + "</text>");

                if (modelOverride != null) {
                    spec.options(OpenAiChatOptions.builder()
                            .model(modelOverride)
                            .build());
                }

                ChatResponse response = spec.call().chatResponse();
                long latencyMs = System.currentTimeMillis() - start;
                AnalysisResponse result = parseResponse(response.getResult().getOutput().getText());
                logUsage(response, latencyMs, modelOverride != null);
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
        try {
            return attemptClassify(request, null);
        } catch (LlmUnavailableException e) {
            log.warn("Primary model exhausted, routing to fallback: {}", fallbackModel);
            return attemptClassify(request, fallbackModel);
        }
    }

    private ClassifyResponse attemptClassify(AnalyzeRequest request, String modelOverride) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                long start = System.currentTimeMillis();

                var spec = chatClient.prompt()
                        .system(classifyPrompt)
                        .user(request.getText());

                if (modelOverride != null) {
                    spec.options(OpenAiChatOptions.builder()
                            .model(modelOverride)
                            .build());
                }

                ChatResponse response = spec.call().chatResponse();
                long latencyMs = System.currentTimeMillis() - start;
                ClassifyResponse result = classifyResponse(response.getResult().getOutput().getText());
                logUsage(response, latencyMs, modelOverride != null);
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

    private void logUsage(ChatResponse response, long latencyMs, boolean isFallback) {
        var usage = response.getMetadata().getUsage();
        log.info("LLM usage — provider: {}, model: {}, input tokens: {}, output tokens: {}, total: {}, latency: {}ms",
                isFallback ? "fallback" : "primary",
                response.getMetadata().getModel(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens(),
                latencyMs);
    }
}
