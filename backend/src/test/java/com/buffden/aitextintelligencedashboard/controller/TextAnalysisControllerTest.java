package com.buffden.aitextintelligencedashboard.controller;

import com.buffden.aitextintelligencedashboard.dto.AnalysisResponse;
import com.buffden.aitextintelligencedashboard.dto.AnalyzeRequest;
import com.buffden.aitextintelligencedashboard.exception.LlmUnavailableException;
import com.buffden.aitextintelligencedashboard.exception.ParseException;
import com.buffden.aitextintelligencedashboard.service.TextAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TextAnalysisController.class)
class TextAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TextAnalysisService textAnalysisService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AnalysisResponse SAMPLE_RESPONSE = AnalysisResponse.builder()
            .summary("AI is changing industries. Companies invest in automation.")
            .sentiment(AnalysisResponse.Sentiment.positive)
            .confidence(0.9)
            .keyTopics(List.of("AI transformation", "automation", "investment"))
            .wordCountEstimate(12)
            .build();

    @Test
    void analyze_validRequest_returns200WithBody() throws Exception {
        when(textAnalysisService.analyze(any())).thenReturn(SAMPLE_RESPONSE);

        AnalyzeRequest request = new AnalyzeRequest();
        request.setText("AI is transforming the world.");

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("AI is changing industries. Companies invest in automation."))
                .andExpect(jsonPath("$.sentiment").value("positive"))
                .andExpect(jsonPath("$.confidence").value(0.9))
                .andExpect(jsonPath("$.key_topics[0]").value("AI transformation"))
                .andExpect(jsonPath("$.word_count_estimate").value(12));
    }

    @Test
    void analyze_blankText_returns400WithErrorCode() throws Exception {
        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void analyze_missingTextField_returns400() throws Exception {
        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void analyze_textExceeds5000Chars_returns400() throws Exception {
        String oversized = "a".repeat(5001);
        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + oversized + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void analyze_llmUnavailable_returns502() throws Exception {
        when(textAnalysisService.analyze(any()))
                .thenThrow(new LlmUnavailableException("unavailable", new RuntimeException()));

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"some text\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("LLM_UNAVAILABLE"));
    }

    @Test
    void analyze_parseError_returns500() throws Exception {
        when(textAnalysisService.analyze(any()))
                .thenThrow(new ParseException("parse fail", "{bad json}", new RuntimeException()));

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"some text\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("PARSE_ERROR"));
    }
}
