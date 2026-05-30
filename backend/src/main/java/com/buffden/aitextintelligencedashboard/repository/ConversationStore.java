package com.buffden.aitextintelligencedashboard.repository;

import com.buffden.aitextintelligencedashboard.config.ConversationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConversationStore {

    private record ConversationEntry(List<Message> messages, Instant createdAt) {}

    private final ConcurrentHashMap<String, ConversationEntry> store = new ConcurrentHashMap<>();

    private final ConversationProperties conversationProperties;

    public ConversationStore(ConversationProperties conversationProperties) {
        this.conversationProperties = conversationProperties;
    }

    public List<Message> getHistory(String conversationId) {
        ConversationEntry entry = store.get(conversationId);
        if (entry == null) return List.of();

        List<Message> all = entry.messages();
        int maxMessages = conversationProperties.getMaxMessages();
        if (all.size() <= maxMessages) return new ArrayList<>(all);

        return new ArrayList<>(all.subList(all.size() - maxMessages, all.size()));
    }

    // compute() is atomic, safe when multiple requests hit the same conversationId concurrently
    public void addMessages(String conversationId, List<Message> newMessages) {
        store.compute(conversationId, (id, existing) -> {
            List<Message> updated = existing != null
                    ? new ArrayList<>(existing.messages())
                    : new ArrayList<>();
            updated.addAll(newMessages);
            Instant createdAt = existing != null ? existing.createdAt() : Instant.now();
            return new ConversationEntry(updated, createdAt);
        });
    }

    public boolean exists(String conversationId) {
        return store.containsKey(conversationId);
    }

    @Scheduled(fixedRateString = "${app.conversation.cleanup-rate-ms}")
    public void cleanupExpired() {
        Instant cutoff = Instant.now().minusSeconds(conversationProperties.getExpirySeconds());
        int removed = 0;

        for (var entry : store.entrySet()) {
            if (entry.getValue().createdAt().isBefore(cutoff)) {
                store.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            log.info("Cleaned up {} expired conversation(s)", removed);
        }
    }
}
