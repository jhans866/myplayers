package com.app.playerservicejava.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class    GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ✅ Helper to build standard error body
    private Map<String, Object> buildError(HttpStatus status, String errorCode,
                                           String message, String path, String correlationId) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", status.value());
        error.put("errorCode", errorCode);
        error.put("message", message);
        error.put("path", path);
        error.put("correlationId", correlationId); // For log tracing
        return error;
    }

    // ✅ 404 - Player not found
    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePlayerNotFound(
            PlayerNotFoundException ex, HttpServletRequest request) {

        String correlationId = UUID.randomUUID().toString();
        LOGGER.warn("[{}] Player not found: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError(HttpStatus.NOT_FOUND, "PLAYER_NOT_FOUND",
                        ex.getMessage(), request.getRequestURI(), correlationId));
    }

    // ✅ 400 - Validation errors (@Valid fields)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String correlationId = UUID.randomUUID().toString();
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        LOGGER.warn("[{}] Validation failed: {}", correlationId, errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                        errors, request.getRequestURI(), correlationId));
    }

    // ✅ 400 - Malformed JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String correlationId = UUID.randomUUID().toString();
        LOGGER.warn("[{}] Malformed JSON: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
                        "Request body is missing or malformed",
                        request.getRequestURI(), correlationId));
    }

    // ✅ 409 - Duplicate player
    @ExceptionHandler(PlayerAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handlePlayerAlreadyExists(
            PlayerAlreadyExistsException ex, HttpServletRequest request) {

        String correlationId = UUID.randomUUID().toString();
        LOGGER.warn("[{}] Duplicate player: {}", correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError(HttpStatus.CONFLICT, "PLAYER_ALREADY_EXISTS",
                        ex.getMessage(), request.getRequestURI(), correlationId));
    }

    // ✅ 500 - Catch all unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String correlationId = UUID.randomUUID().toString();
        LOGGER.error("[{}] Unexpected error: {}", correlationId, ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred. CorrelationId: " + correlationId,
                        request.getRequestURI(), correlationId));
    }
}
