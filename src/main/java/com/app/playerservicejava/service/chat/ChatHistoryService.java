package com.app.playerservicejava.service.chat;

import com.app.playerservicejava.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryService.class);

    private static final int MAX_HISTORY_SIZE = 20;       // max messages per session
    private static final long SESSION_TTL_MINUTES = 30;   // expire after 30 min inactivity

    // sessionId → list of messages
    private final Map<String, List<Message>> sessionHistory = new ConcurrentHashMap<>();

    // sessionId → last activity time (for TTL)
    private final Map<String, Instant> sessionLastAccess = new ConcurrentHashMap<>();

    // ✅ Add a message to session history
    public void addMessage(String sessionId, Message message) {
        evictExpiredSessions(); // cleanup on every write

        sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        List<Message> history = sessionHistory.get(sessionId);

        // Trim if too large
        if (history.size() >= MAX_HISTORY_SIZE) {
            history.remove(0); // remove oldest
        }

        history.add(message);
        sessionLastAccess.put(sessionId, Instant.now());
        LOGGER.debug("Session {} now has {} messages", sessionId, history.size());
    }

    // ✅ Get full history for a session
    public List<Message> getHistory(String sessionId) {
        sessionLastAccess.put(sessionId, Instant.now());
        return sessionHistory.getOrDefault(sessionId, new ArrayList<>());
    }

    // ✅ Clear a specific session
    public void clearSession(String sessionId) {
        sessionHistory.remove(sessionId);
        sessionLastAccess.remove(sessionId);
        LOGGER.info("Cleared session: {}", sessionId);
    }

    // ✅ Get session count (for monitoring)
    public int getActiveSessionCount() {
        return sessionHistory.size();
    }

    // ✅ TTL eviction - remove sessions inactive > SESSION_TTL_MINUTES
    private void evictExpiredSessions() {
        Instant cutoff = Instant.now().minusSeconds(SESSION_TTL_MINUTES * 60);
        sessionLastAccess.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoff)) {
                sessionHistory.remove(entry.getKey());
                LOGGER.info("Evicted expired session: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
