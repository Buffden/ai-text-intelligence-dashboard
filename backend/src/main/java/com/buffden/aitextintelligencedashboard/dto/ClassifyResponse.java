package com.buffden.aitextintelligencedashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyResponse {
    private double confidence;
    private String reasoning;
    private Category category;

    public enum Category {
        technology, politics, sports, business, health, other
    };
}
