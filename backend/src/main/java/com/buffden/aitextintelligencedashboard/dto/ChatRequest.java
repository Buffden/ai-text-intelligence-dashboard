package com.buffden.aitextintelligencedashboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    private String conversationId;

    @NotBlank(message = "Message must not be blank")
    private String message;
}
