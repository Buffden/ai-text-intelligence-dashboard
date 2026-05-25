package com.buffden.aitextintelligencedashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalysisResponse {
    private String summary;
    private Sentiment sentiment;
    private double confidence;
    private List<String> keyTopics;
    private int wordCountEstimate;

    public enum Sentiment {
        positive, negative, neutral
    }
}
