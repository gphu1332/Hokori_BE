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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.LazyInitializationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
            errorMessage.append(fieldName).append(" ").append(message).append("; ");
        });
        
        // Return ApiResponse format for consistency
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", errorMessage.toString().trim());
        response.put("data", null);
        response.put("errors", errors);
        
        logger.warn("Validation failed: {}", errors);
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        String message = ex.getMessage();
        String detailedMessage = "Invalid JSON format in request body";
        
        // Extract more specific error message
        if (message != null) {
            if (message.contains("JSON parse error")) {
                detailedMessage = "JSON parse error: Invalid JSON format. Please check your request body.";
            } else if (message.contains("Unexpected character")) {
                detailedMessage = "JSON parse error: Invalid character in JSON. " +
                        "Make sure your request body is valid JSON and Content-Type header is 'application/json'.";
            } else if (message.contains("Expected")) {
                detailedMessage = "JSON parse error: " + message;
            }
        }
        
        response.put("message", detailedMessage);
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false).replace("uri=", ""));
        
        // Add helpful suggestions
        Map<String, String> suggestions = new HashMap<>();
        suggestions.put("checkContentType", "Ensure Content-Type header is 'application/json'");
        suggestions.put("checkJsonFormat", "Verify request body is valid JSON format");
        suggestions.put("checkSpecialChars", "Check for unescaped special characters in strings");
        suggestions.put("checkTrailingComma", "Remove trailing commas in JSON objects/arrays");
        response.put("suggestions", suggestions);
        
        logger.warn("JSON parse error: {}", message);
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        String contentType = ex.getContentType() != null ? ex.getContentType().toString() : "unknown";
        String supportedTypes = ex.getSupportedMediaTypes() != null && !ex.getSupportedMediaTypes().isEmpty()
                ? ex.getSupportedMediaTypes().toString()
                : "multipart/form-data";
        
        String detailedMessage;
        if (contentType.contains("application/json")) {
            detailedMessage = String.format(
                "Content-Type '%s' is not supported for file upload endpoints. " +
                "Please use 'multipart/form-data' instead. " +
                "Use FormData to append the file and let the browser set Content-Type automatically.",
                contentType);
        } else {
            detailedMessage = String.format(
                "Content-Type '%s' is not supported. Supported types: %s",
                contentType, supportedTypes);
        }
        
        response.put("message", detailedMessage);
        response.put("status", "error");
        response.put("errorCode", "UNSUPPORTED_MEDIA_TYPE");
        response.put("receivedContentType", contentType);
        response.put("supportedContentTypes", supportedTypes);
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false).replace("uri=", ""));
        
        // Add helpful suggestions for file upload
        Map<String, String> suggestions = new HashMap<>();
        suggestions.put("useFormData", "Use FormData to append file: const formData = new FormData(); formData.append('file', file);");
        suggestions.put("dontSetContentType", "Do NOT manually set Content-Type header when using FormData - browser will set it automatically");
        suggestions.put("useMultipart", "Ensure Content-Type is 'multipart/form-data' (browser sets this automatically with FormData)");
        response.put("suggestions", suggestions);
        
        logger.warn("HttpMediaTypeNotSupportedException: Content-Type '{}' not supported. Supported: {}", 
            contentType, supportedTypes);
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "Resource not found");
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        logger.debug("EntityNotFoundException: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<Map<String, Object>> handleLazyInitializationException(
            LazyInitializationException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Lazy loading error: " + (ex.getMessage() != null ? ex.getMessage() : "no Session"));
        response.put("status", "error");
        response.put("errorType", "LazyInitializationException");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        // Log full stack trace for debugging
        logger.error("LazyInitializationException occurred - this indicates a missing JOIN FETCH in repository query", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        String parameterName = ex.getName();
        String parameterValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        
        // Check if FE is sending string "null" instead of actual null or number
        if ("null".equals(parameterValue)) {
            response.put("message", String.format(
                "Invalid parameter '%s': Received string 'null' instead of a valid %s. " +
                "Please check that you are passing a valid %s value (not the string 'null'). " +
                "Example: testId should be a number like 123, not the string 'null'.",
                parameterName, requiredType, requiredType));
            response.put("parameter", parameterName);
            response.put("receivedValue", parameterValue);
            response.put("expectedType", requiredType);
            response.put("suggestion", "Make sure you are passing the actual testId value, not null or the string 'null'");
        } else {
            response.put("message", String.format(
                "Invalid parameter '%s': Cannot convert '%s' to %s. Please provide a valid %s value.",
                parameterName, parameterValue, requiredType, requiredType));
            response.put("parameter", parameterName);
            response.put("receivedValue", parameterValue);
            response.put("expectedType", requiredType);
        }
        
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false));
        
        logger.warn("MethodArgumentTypeMismatchException: {} = '{}' (expected {})", 
            parameterName, parameterValue, requiredType);
        
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
                    "Please ensure the database constraint allows all CourseStatus values (DRAFT, PENDING_APPROVAL, REJECTED, PUBLISHED, FLAGGED, ARCHIVED). " +
                    "Call POST /api/admin/database/fix-course-status-constraint to fix.");
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