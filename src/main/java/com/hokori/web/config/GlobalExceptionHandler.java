package com.hokori.web.config;

import com.hokori.web.exception.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        response.put("message", "Validation failed");
        response.put("status", "error");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // Handle LOB stream errors specifically
        String message = ex.getMessage();
        if (message != null && (message.contains("lob stream") || message.contains("LOB"))) {
            response.put("message", "Unable to access lob stream");
        } else {
            response.put("message", message != null ? message : "An error occurred");
        }
        
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        // Don't serialize exception details to avoid triggering LOB loading
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invalid argument: " + ex.getMessage());
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "An unexpected error occurred");
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        // Log the actual exception for debugging
        logger.error("Unexpected error occurred", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAIServiceException(
            AIServiceException ex, WebRequest request) {
        logger.error("AI Service Error [{}]: {}", ex.getServiceName(), ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("status", "error");
        response.put("errorCode", ex.getErrorCode());
        response.put("serviceName", ex.getServiceName());
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getReason() != null ? ex.getReason() : ex.getMessage());
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        logger.debug("ResponseStatusException: {} - {}", ex.getStatusCode(), ex.getReason());
        
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        String resourcePath = ex.getResourcePath();
        
        // Check if URL has duplicate /api/api
        if (resourcePath != null && resourcePath.contains("/api/api")) {
            response.put("message", "Invalid URL: duplicate '/api' detected. Please check your API base URL configuration.");
            response.put("suggestedPath", resourcePath.replace("/api/api", "/api"));
        } else {
            response.put("message", "Resource not found: " + resourcePath);
        }
        
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        logger.warn("NoResourceFoundException: {}", resourcePath);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        String message = ex.getMessage();
        
        // Check for specific constraint violations
        if (message != null) {
            if (message.contains("course_status_check")) {
                response.put("message", "Database constraint error: Course status constraint violation. " +
                    "Please ensure the database constraint allows 'PENDING_APPROVAL' status.");
                response.put("errorCode", "COURSE_STATUS_CONSTRAINT_ERROR");
            } else if (message.contains("unique constraint") || message.contains("duplicate key")) {
                response.put("message", "Duplicate entry: This record already exists");
            } else if (message.contains("foreign key constraint")) {
                response.put("message", "Referential integrity error: Related record not found");
            } else {
                response.put("message", "Database constraint violation: " + message);
            }
        } else {
            response.put("message", "Database constraint violation");
        }
        
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        logger.error("DataIntegrityViolationException: {}", message, ex);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Custom exception classes
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}