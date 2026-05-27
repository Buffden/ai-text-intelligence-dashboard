package com.buffden.aitextintelligencedashboard.service;

import com.buffden.aitextintelligencedashboard.dto.AnalysisResponse;
import com.buffden.aitextintelligencedashboard.dto.AnalyzeRequest;
import com.buffden.aitextintelligencedashboard.dto.ClassifyResponse;
import com.buffden.aitextintelligencedashboard.exception.LlmUnavailableException;
import com.buffden.aitextintelligencedashboard.exception.ParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextAnalysisServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private TextAnalysisService service;

    private static final String VALID_JSON = """
            {
              "summary": "AI is changing industries. Companies invest heavily in automation.",
              "sentiment": "positive",
              "confidence": 0.9,
              "key_topics": ["AI transformation", "automation", "investment"],
              "word_count_estimate": 12
            }
            """;

    private static final String VALID_CLASSIFY_JSON = """
            {
              "reasoning": "The text discusses an AI model release targeting developers.",
              "category": "technology",
              "confidence": 0.95
            }
            """;

    @BeforeEach
    void setUp() throws Exception {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        Resource analyzePrompt = new ByteArrayResource("You are a text analysis assistant.".getBytes(StandardCharsets.UTF_8));
        Resource classifyPrompt = new ByteArrayResource("You are a text classification assistant.".getBytes(StandardCharsets.UTF_8));
        service = new TextAnalysisService(chatClientBuilder, new ObjectMapper(), classifyPrompt, analyzePrompt, "gpt-4o-mini");
    }

    private AnalyzeRequest request(String text) {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setText(text);
        return req;
    }

    private ChatResponse stubChatResponse(String rawText) {
        ChatResponse response = mock(ChatResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(response.getResult().getOutput().getText()).thenReturn(rawText);
        when(response.getMetadata().getModel()).thenReturn("gpt-4o");
        when(response.getMetadata().getUsage().getPromptTokens()).thenReturn(20);
        when(response.getMetadata().getUsage().getCompletionTokens()).thenReturn(80);
        when(response.getMetadata().getUsage().getTotalTokens()).thenReturn(100);
        return response;
    }

    @Test
    void analyze_validResponse_returnsMappedResult() {
        ChatResponse chatResponse = stubChatResponse(VALID_JSON);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenReturn(chatResponse);

        AnalysisResponse result = service.analyze(request("AI is transforming the world."));

        assertThat(result.getSummary()).isEqualTo("AI is changing industries. Companies invest heavily in automation.");
        assertThat(result.getSentiment()).isEqualTo(AnalysisResponse.Sentiment.positive);
        assertThat(result.getConfidence()).isEqualTo(0.9);
        assertThat(result.getKeyTopics()).containsExactly("AI transformation", "automation", "investment");
        assertThat(result.getWordCountEstimate()).isEqualTo(12);
    }

    @Test
    void analyze_markdownWrappedJson_stripsAndParses() {
        String wrapped = "```json\n" + VALID_JSON + "\n```";
        ChatResponse chatResponse = stubChatResponse(wrapped);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenReturn(chatResponse);

        AnalysisResponse result = service.analyze(request("some text"));

        assertThat(result.getSentiment()).isEqualTo(AnalysisResponse.Sentiment.positive);
        assertThat(result.getKeyTopics()).hasSize(3);
    }

    @Test
    void analyze_firstAttemptFails_retriesAndSucceeds() {
        ChatResponse chatResponse = stubChatResponse(VALID_JSON);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(chatResponse);

        AnalysisResponse result = service.analyze(request("some text"));

        assertThat(result.getSummary()).isNotBlank();
    }

    @Test
    void analyze_bothAttemptsFail_throwsLlmUnavailableException() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.analyze(request("some text")))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void analyze_malformedJsonOnBothAttempts_throwsParseException() {
        ChatResponse chatResponse = mock(ChatResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(chatResponse.getResult().getOutput().getText()).thenReturn("not valid json at all");
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenReturn(chatResponse);

        assertThatThrownBy(() -> service.analyze(request("some text")))
                .isInstanceOf(ParseException.class)
                .satisfies(e -> assertThat(((ParseException) e).getRawResponse())
                        .isEqualTo("not valid json at all"));
    }

    @Test
    void classify_validResponse_returnsMappedResult() {
        ChatResponse chatResponse = stubChatResponse(VALID_CLASSIFY_JSON);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenReturn(chatResponse);

        ClassifyResponse result = service.classify(request("OpenAI released a new model."));

        assertThat(result.getReasoning()).isEqualTo("The text discusses an AI model release targeting developers.");
        assertThat(result.getConfidence()).isEqualTo(0.95);
    }

    @Test
    void classify_markdownWrappedJson_stripsAndParses() {
        String wrapped = "```json\n" + VALID_CLASSIFY_JSON + "\n```";
        ChatResponse chatResponse = stubChatResponse(wrapped);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenReturn(chatResponse);

        ClassifyResponse result = service.classify(request("some text"));

        assertThat(result.getReasoning()).isNotBlank();
        assertThat(result.getConfidence()).isEqualTo(0.95);
    }

    @Test
    void classify_firstAttemptFails_retriesAndSucceeds() {
        ChatResponse chatResponse = stubChatResponse(VALID_CLASSIFY_JSON);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(chatResponse);

        ClassifyResponse result = service.classify(request("some text"));

        assertThat(result.getReasoning()).isNotBlank();
    }

    @Test
    void classify_bothAttemptsFail_throwsLlmUnavailableException() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.classify(request("some text")))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void classify_malformedJsonOnBothAttempts_throwsParseException() {
        ChatResponse chatResponse = mock(ChatResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(chatResponse.getResult().getOutput().getText()).thenReturn("not valid json at all");
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenReturn(chatResponse);

        assertThatThrownBy(() -> service.classify(request("some text")))
                .isInstanceOf(ParseException.class)
                .satisfies(e -> assertThat(((ParseException) e).getRawResponse())
                        .isEqualTo("not valid json at all"));
    }

    @Test
    void analyze_allSentimentValues_deserializeCorrectly() {
        for (AnalysisResponse.Sentiment sentiment : AnalysisResponse.Sentiment.values()) {
            String json = """
                    {
                      "summary": "Test. Detail.",
                      "sentiment": "%s",
                      "confidence": 0.8,
                      "key_topics": ["a", "b", "c"],
                      "word_count_estimate": 5
                    }
                    """.formatted(sentiment.name());

            ChatResponse chatResponse = stubChatResponse(json);
            when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                    .thenReturn(chatResponse);

            AnalysisResponse result = service.analyze(request("text"));
            assertThat(result.getSentiment()).isEqualTo(sentiment);
        }
    }
}
