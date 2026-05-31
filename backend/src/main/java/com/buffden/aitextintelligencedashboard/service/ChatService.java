package com.buffden.aitextintelligencedashboard.service;

import com.buffden.aitextintelligencedashboard.dto.ChatHistoryItem;
import com.buffden.aitextintelligencedashboard.dto.ChatReply;
import com.buffden.aitextintelligencedashboard.dto.ChatRequest;
import com.buffden.aitextintelligencedashboard.dto.ConversationSummary;
import com.buffden.aitextintelligencedashboard.repository.ConversationStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationStore conversationStore;
    private final String systemPrompt;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       ConversationStore conversationStore,
                       @Value("classpath:prompts/chat-system.st") Resource promptResource) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.conversationStore = conversationStore;
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public ChatReply chat(ChatRequest request) {
        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        List<Message> history = conversationStore.getHistory(conversationId);

        var response = chatClient.prompt()
                .system(systemPrompt)
                .messages(history)
                .user(request.getMessage())
                .call()
                .chatResponse();

        String reply = response.getResult().getOutput().getText();
        var usage = response.getMetadata().getUsage();

        log.info("Chat, conversationId: {}, input tokens: {}, output tokens: {}, total: {}",
                conversationId,
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens());

        conversationStore.addMessages(conversationId, List.of(
                new UserMessage(request.getMessage()),
                new AssistantMessage(reply)
        ));

        return new ChatReply(conversationId, reply);
    }

    public List<ChatHistoryItem> getHistory(String conversationId) {
        return conversationStore.getHistory(conversationId).stream()
                .map(msg -> new ChatHistoryItem(
                        msg.getMessageType().getValue(),
                        msg.getText()))
                .toList();
    }

    public void chatStream(ChatRequest request, SseEmitter emitter) {
        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        List<Message> history = conversationStore.getHistory(conversationId);
        StringBuilder fullReply = new StringBuilder();

        // Send conversationId first so the frontend can persist it before tokens arrive
        try {
            emitter.send(SseEmitter.event().name("conversation-id").data(conversationId));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return;
        }

        chatClient.prompt()
                .system(systemPrompt)
                .messages(history)
                .user(request.getMessage())
                .stream()
                .content()
                .subscribe(
                        token -> {
                            try {
                                fullReply.append(token);
                                emitter.send(SseEmitter.event().data(token));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event().name("error")
                                        .data(error.getMessage() != null ? error.getMessage() : "Unknown error"));
                            } catch (IOException e) {
                                // client already disconnected
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            conversationStore.addMessages(conversationId, List.of(
                                    new UserMessage(request.getMessage()),
                                    new AssistantMessage(fullReply.toString())
                            ));
                            log.info("Chat stream complete, conversationId: {}, tokens: {}", conversationId, fullReply.length());
                            try {
                                emitter.send(SseEmitter.event().data("[DONE]"));
                            } catch (IOException e) {
                                // client already disconnected
                            }
                            emitter.complete();
                        }
                );
    }

    public List<ConversationSummary> listConversations() {
        return conversationStore.listAll().stream()
                .map(snapshot -> {
                    String title = snapshot.messages().stream()
                            .filter(m -> m instanceof UserMessage)
                            .findFirst()
                            .map(m -> {
                                String text = m.getText();
                                return text.length() > 60 ? text.substring(0, 60) + "..." : text;
                            })
                            .orElse("New conversation");
                    return new ConversationSummary(
                            snapshot.id(), title, snapshot.createdAt(), snapshot.messages().size());
                })
                .sorted(Comparator.comparing(ConversationSummary::getCreatedAt).reversed())
                .toList();
    }
}
