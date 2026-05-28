package com.buffden.aitextintelligencedashboard.service;

import com.buffden.aitextintelligencedashboard.config.LlmProperties;
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
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Slf4j
@Service
public class TextAnalysisService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String systemPrompt;
    private final String classifyPrompt;
    private final LlmProperties llmProperties;
    private final String fallbackModel;
    private final int maxAttempts;
    private final long baseDelayMs;
    private final Random random = new Random();

    public TextAnalysisService(ChatClient.Builder chatClientBuilder,
                                ObjectMapper objectMapper,
                                @Value("classpath:prompts/classify-system.st") Resource classifyPromptResource,
                                @Value("classpath:prompts/analyze-system.st") Resource promptResource,
                                LlmProperties llmProperties) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.classifyPrompt = classifyPromptResource.getContentAsString(StandardCharsets.UTF_8);
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        this.llmProperties = llmProperties;
        this.fallbackModel = llmProperties.getModels().getFallback().get(0).getName();
        this.maxAttempts = llmProperties.getRetry().getMaxAttempts();
        this.baseDelayMs = llmProperties.getRetry().getBaseDelayMs();
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

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
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

            } catch (ParseException e) {
                throw e;
            } catch (HttpClientErrorException e) {
                lastException = e;
                log.warn("LLM attempt {}/{} failed with HTTP {}: {}", attempt, maxAttempts, e.getStatusCode().value(), e.getMessage());
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt, e);
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("LLM attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt, null);
                }
            }
        }

        throw new LlmUnavailableException("LLM service unavailable after " + maxAttempts + " attempts", lastException);
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

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
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

            } catch (ParseException e) {
                throw e;
            } catch (HttpClientErrorException e) {
                lastException = e;
                log.warn("LLM attempt {}/{} failed with HTTP {}: {}", attempt, maxAttempts, e.getStatusCode().value(), e.getMessage());
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt, e);
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("LLM attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt, null);
                }
            }
        }
        throw new LlmUnavailableException("LLM service unavailable after " + maxAttempts + " attempts", lastException);
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

    private void sleepBeforeRetry(int attempt, HttpClientErrorException e) {
        try {
            long delay;
            String retryAfter = (e != null && e.getStatusCode().value() == 429 && e.getResponseHeaders() != null)
                    ? e.getResponseHeaders().getFirst("Retry-After")
                    : null;

            long backoff = baseDelayMs * (1L << (attempt - 1)); // baseDelay * 2^(attempt-1)
            if (retryAfter != null) {
                try {
                    delay = Long.parseLong(retryAfter) * 1000L;
                    log.info("Respecting Retry-After header: waiting {}ms before next attempt", delay);
                } catch (NumberFormatException ex) {
                    delay = (long) (random.nextDouble() * backoff);
                    log.info("Backoff (Retry-After unparseable): waiting {}ms before attempt {}", delay, attempt + 1);
                }
            } else {
                delay = (long) (random.nextDouble() * backoff);    // full jitter: random(0, backoff)
                log.info("Backoff: waiting {}ms before attempt {}", delay, attempt + 1);
            }

            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("Retry interrupted", ie);
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
