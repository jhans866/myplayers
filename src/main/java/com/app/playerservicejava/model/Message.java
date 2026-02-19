package com.app.playerservicejava.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class Message {
    private String role;    // "user" or "assistant"
    private String content;
    private Instant timestamp;

    // Convenience constructors
    public static Message user(String content) {
        return new Message("user", content, Instant.now());
    }

    public static Message assistant(String content) {
        return new Message("assistant", content, Instant.now());
    }
}
