package com.app.playerservicejava.controller.chat;

import com.app.playerservicejava.model.ChatRequest;
import com.app.playerservicejava.model.Message;
import com.app.playerservicejava.model.ModelDto;
import com.app.playerservicejava.service.chat.ChatClientService;
import com.app.playerservicejava.service.chat.ChatHistoryService;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "v1/chat", produces = { MediaType.APPLICATION_JSON_VALUE })
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatClientService chatClientService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    // @PostMapping
    //public @ResponseBody String chat() throws OllamaBaseException, IOException, InterruptedException {

    @PostMapping
    public String chat() throws OllamaBaseException, IOException, InterruptedException {
        return chatClientService.chat();
    }

    /*@GetMapping("/list-models")
    public ResponseEntity<List<Model>> listModels() throws OllamaBaseException, IOException, URISyntaxException, InterruptedException {
        List<Model> models = chatClientService.listModels();
        return ResponseEntity.ok(models);
    }*/
    // ✅ Handle validation errors (injection / empty prompt)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidationError(IllegalArgumentException e) {
        LOGGER.warn("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @PostMapping("/prompt")
    public ResponseEntity<String> chatWithPrompt(@RequestBody ChatRequest request)
            throws OllamaBaseException, IOException, InterruptedException {
        LOGGER.info("Received custom prompt: {}", request.getPrompt());
        String response = chatClientService.chat(request.getPrompt());
        return ResponseEntity.ok(response);
    }

    // ✅ Story 17 - NEW: chat with session history
    @PostMapping("/session")
    public ResponseEntity<String> chatWithSession(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId)
            throws OllamaBaseException, IOException, InterruptedException {

        // Generate sessionId if not provided
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
            LOGGER.info("Generated new sessionId: {}", sessionId);
        }

        // Get existing history
        List<Message> history = chatHistoryService.getHistory(sessionId);

        // Call Ollama with full history context
        String response = chatClientService.chatWithHistory(sessionId, request.getPrompt(), history);

        // Save user message + assistant response to history
        chatHistoryService.addMessage(sessionId, Message.user(request.getPrompt()));
        chatHistoryService.addMessage(sessionId, Message.assistant(response));

        return ResponseEntity.ok()
                .header("X-Session-Id", sessionId)    // ✅ Return sessionId in response header
                .body(response);
    }

    // ✅ Story 17 - GET session history
    @GetMapping("/session/{sessionId}/history")
    public ResponseEntity<List<Message>> getHistory(@PathVariable String sessionId) {
        List<Message> history = chatHistoryService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    // ✅ Story 17 - DELETE session history
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<String> clearSession(@PathVariable String sessionId) {
        chatHistoryService.clearSession(sessionId);
        return ResponseEntity.ok("Session cleared: " + sessionId);
    }

    @GetMapping("/list-models")
    public ResponseEntity<List<ModelDto>> listModels()
            throws OllamaBaseException, IOException, URISyntaxException, InterruptedException {
        LOGGER.info("Fetching available Ollama models");
        List<ModelDto> models = chatClientService.listModels()
                .stream()
                .map(m -> new ModelDto(
                        m.getModelName(),
                        m.getSize(),
                        m.getModifiedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(models);
    }


}
