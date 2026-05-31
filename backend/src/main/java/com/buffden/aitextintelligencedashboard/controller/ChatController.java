package com.buffden.aitextintelligencedashboard.controller;

import com.buffden.aitextintelligencedashboard.dto.ChatHistoryItem;
import com.buffden.aitextintelligencedashboard.dto.ChatReply;
import com.buffden.aitextintelligencedashboard.dto.ChatRequest;
import com.buffden.aitextintelligencedashboard.dto.ConversationSummary;
import com.buffden.aitextintelligencedashboard.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatReply chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        chatService.chatStream(request, emitter);
        return emitter;
    }

    // returns full conversation history
    // Used by the frontend on page reload to re-hydrate the chat UI from localStorage conversationId
    @GetMapping("/{conversationId}/history")
    public List<ChatHistoryItem> history(@PathVariable String conversationId) {
        return chatService.getHistory(conversationId);
    }
    // returns all active conversations
    @GetMapping("/conversations")
    public List<ConversationSummary> conversations() {
        return chatService.listConversations();
    }
}
