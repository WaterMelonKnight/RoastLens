package com.roastlens.controller;

import com.roastlens.content.ContentLanguageConflictException;
import com.roastlens.connector.finstream.FinStreamClientException;
import com.roastlens.connector.finstream.FinStreamEventNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ContentLanguageConflictException.class)
    public ResponseEntity<Map<String, String>> handleContentLanguageConflict(ContentLanguageConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(error -> error.getField()))
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed");
        return ResponseEntity.badRequest().body(error(message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleUpstreamFailure(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(ex.getMessage()));
    }

    @ExceptionHandler(FinStreamEventNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFinStreamNotFound(FinStreamEventNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
    }

    @ExceptionHandler(FinStreamClientException.class)
    public ResponseEntity<Map<String, String>> handleFinStreamFailure(FinStreamClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(ex.getMessage()));
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message == null ? "Unexpected error" : message);
    }
}
