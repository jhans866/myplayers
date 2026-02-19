package com.app.playerservicejava.service.chat;

import com.app.playerservicejava.model.Message;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.Model;
import io.github.ollama4j.models.OllamaResult;
import io.github.ollama4j.types.OllamaModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.github.ollama4j.utils.OptionsBuilder;
import io.github.ollama4j.utils.PromptBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@Service
public class ChatClientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatClientService.class);
    private static final List<String> BLOCKED_PATTERNS = List.of(
            "ignore previous", "ignore all", "disregard", "jailbreak",
            "system prompt", "act as", "you are now", "forget instructions"
    );

    @Autowired
    private OllamaAPI ollamaAPI;

    public List<Model> listModels() throws OllamaBaseException, IOException, URISyntaxException, InterruptedException {
        List<Model> models = ollamaAPI.listModels();
        return models;
    }

    public String chat() throws OllamaBaseException, IOException, InterruptedException {
        String model = OllamaModelType.TINYLLAMA;

        // https://ollama4j.github.io/ollama4j/intro
        PromptBuilder promptBuilder =
                new PromptBuilder()
                        .addLine("Recite a haiku about recursion.");

        boolean raw = false;
        OllamaResult response = ollamaAPI.generate(model, promptBuilder.build(), raw, new OptionsBuilder().build());
        return response.getResponse();
    }

    // ✅ Accepts custom prompt from request body
    public String chat(String prompt) throws OllamaBaseException, IOException, InterruptedException {

        // Null / empty check
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        // Length check
        if (prompt.length() > 1000) {
            throw new IllegalArgumentException("Prompt too long. Max 1000 characters.");
        }

        // Prompt injection check
        String lower = prompt.toLowerCase();
        for (String blocked : BLOCKED_PATTERNS) {
            if (lower.contains(blocked)) {
                LOGGER.warn("Blocked prompt injection attempt: {}", prompt);
                throw new IllegalArgumentException("Invalid prompt detected");
            }
        }

        LOGGER.info("Sending custom prompt to Ollama: {}", prompt);

        String model = OllamaModelType.TINYLLAMA;
        PromptBuilder promptBuilder = new PromptBuilder().addLine(prompt);
        OllamaResult response = ollamaAPI.generate(model, promptBuilder.build(), false, new OptionsBuilder().build());
        return response.getResponse();
    }

    // ✅ Chat with full session history as context
    public String chatWithHistory(String sessionId, String prompt, List<Message> history)
            throws OllamaBaseException, IOException, InterruptedException {

        // Validate
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }
        if (prompt.length() > 1000) {
            throw new IllegalArgumentException("Prompt too long. Max 1000 characters.");
        }

        // Injection check
        String lower = prompt.toLowerCase();
        for (String blocked : BLOCKED_PATTERNS) {
            if (lower.contains(blocked)) {
                LOGGER.warn("Blocked prompt injection in session {}: {}", sessionId, prompt);
                throw new IllegalArgumentException("Invalid prompt detected");
            }
        }

        // Build prompt with full conversation history
        PromptBuilder promptBuilder = new PromptBuilder();
        for (Message msg : history) {
            if ("user".equals(msg.getRole())) {
                promptBuilder.addLine("User: " + msg.getContent());
            } else {
                promptBuilder.addLine("Assistant: " + msg.getContent());
            }
        }
        promptBuilder.addLine("User: " + prompt);
        promptBuilder.addLine("Assistant:");

        LOGGER.info("Session {} sending prompt with {} history messages", sessionId, history.size());

        String model = OllamaModelType.TINYLLAMA;
        OllamaResult response = ollamaAPI.generate(
                model,
                promptBuilder.build(),
                false,
                new OptionsBuilder().build()
        );
        return response.getResponse();
    }
}




