package com.buffden.aitextintelligencedashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnalyzeRequest {
    @NotBlank(message = "text must not be blank")
    @Size(max = 5000, message = "text must not exceed 5000 characters")
    private String text;
}
