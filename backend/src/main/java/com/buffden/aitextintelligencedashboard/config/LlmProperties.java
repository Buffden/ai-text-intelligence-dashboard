package com.buffden.aitextintelligencedashboard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private ModelConfig models;
    private RetryConfig retry;
    private TimeoutConfig timeout;

    @Data
    public static class ModelConfig {
        private ModelPricing primary;
        private List<ModelPricing> fallback;
    }

    @Data
    public static class ModelPricing {
        private String name;
        private double inputPricePerThousandTokens;
        private double outputPricePerThousandTokens;
    }

    @Data
    public static class RetryConfig {
        private int maxAttempts;
        private long baseDelayMs;
    }

    @Data
    public static class TimeoutConfig {
        private int connectMs;
        private int readMs;
    }
}
