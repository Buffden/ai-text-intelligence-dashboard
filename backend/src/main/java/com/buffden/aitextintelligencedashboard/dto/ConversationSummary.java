package com.buffden.aitextintelligencedashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ConversationSummary {
    private String id;
    private String title;
    private Instant createdAt;
    private int messageCount;
}
