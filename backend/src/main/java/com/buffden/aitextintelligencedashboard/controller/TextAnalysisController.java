package com.buffden.aitextintelligencedashboard.controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.buffden.aitextintelligencedashboard.service.TextAnalysisService;
import com.buffden.aitextintelligencedashboard.dto.AnalyzeRequest;
import com.buffden.aitextintelligencedashboard.dto.AnalysisResponse;
import com.buffden.aitextintelligencedashboard.dto.ClassifyResponse;

@RestController
@RequestMapping("/api")
public class TextAnalysisController {
    private final TextAnalysisService textAnalysisService;

    public TextAnalysisController(TextAnalysisService textAnalysisService) {
        this.textAnalysisService = textAnalysisService;
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        return textAnalysisService.analyze(request);
    }

    @PostMapping("/classify")
    public ClassifyResponse classify(@Valid @RequestBody AnalyzeRequest request) {
        return textAnalysisService.classify(request);
    }
}