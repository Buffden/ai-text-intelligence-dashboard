package com.buffden.aitextintelligencedashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    private String error;
    private ErrorCode code;
}
