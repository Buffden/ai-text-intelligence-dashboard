package com.buffden.aitextintelligencedashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatHistoryItem {
    private String role;
    private String content;
}
