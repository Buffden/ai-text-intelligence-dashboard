package com.buffden.aitextintelligencedashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String summary;
    private Sentiment sentiment;
    private double confidence;
    @JsonProperty("key_topics")
    private List<String> keyTopics;
    @JsonProperty("word_count_estimate")
    private int wordCountEstimate;

    public enum Sentiment {
        positive, negative, neutral
    }
}
