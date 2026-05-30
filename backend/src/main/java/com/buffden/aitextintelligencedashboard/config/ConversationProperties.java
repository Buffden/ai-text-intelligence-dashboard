package com.buffden.aitextintelligencedashboard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.conversation")
public class ConversationProperties {
    private int maxMessages;
    private long expirySeconds;
}
