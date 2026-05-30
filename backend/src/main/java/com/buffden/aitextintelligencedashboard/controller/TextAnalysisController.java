package com.buffden.aitextintelligencedashboard.controller;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.validation.Valid;
import com.buffden.aitextintelligencedashboard.service.TextAnalysisService;
import com.buffden.aitextintelligencedashboard.dto.AnalyzeRequest;
import com.buffden.aitextintelligencedashboard.dto.AnalysisResponse;
import com.buffden.aitextintelligencedashboard.dto.ClassifyResponse;
import java.io.IOException;

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

    @GetMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeStream(@RequestParam String text) {
        SseEmitter emitter = new SseEmitter(0L);

        textAnalysisService.analyzeStream(text)
                .subscribe(token -> {
                            try {
                                emitter.send(SseEmitter.event().data(String.valueOf(token)));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event().name("error").data(error.getMessage() != null ? error.getMessage() : "Unknown error"));
                            } catch (IOException e) {
                                // client already disconnected
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().data("[DONE]"));
                            } catch (IOException e) {
                                // client already disconnected
                            }
                            emitter.complete();
                        }
                );

        return emitter;
    }
}