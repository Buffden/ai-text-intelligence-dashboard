package com.buffden.aitextintelligencedashboard.service;

import com.buffden.aitextintelligencedashboard.dto.ChatHistoryItem;
import com.buffden.aitextintelligencedashboard.dto.ChatReply;
import com.buffden.aitextintelligencedashboard.dto.ChatRequest;
import com.buffden.aitextintelligencedashboard.repository.ConversationStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
}
