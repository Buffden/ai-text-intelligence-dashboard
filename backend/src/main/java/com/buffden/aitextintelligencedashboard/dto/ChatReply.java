package com.buffden.aitextintelligencedashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatReply {
    private String conversationId;
    private String reply;
}
