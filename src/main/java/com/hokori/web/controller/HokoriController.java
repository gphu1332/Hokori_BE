package com.hokori.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Hokori API", description = "API endpoints for Hokori application")
@CrossOrigin(origins = "*") // Allow frontend from anywhere
public class HokoriController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the API is running")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Hokori API is running!");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("/hello")
    @Operation(summary = "Hello endpoint", description = "Simple greeting endpoint")
    public Map<String, String> hello(@RequestParam(defaultValue = "World") String name) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello " + name + "!");
        response.put("greeting", "Welcome to Hokori API");
        return response;
    }

    @PostMapping("/echo")
    @Operation(summary = "Echo endpoint", description = "Echo back the received data")
    public Map<String, Object> echo(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("received", data);
        response.put("echoed_at", LocalDateTime.now());
        response.put("status", "success");
        return response;
    }

    @GetMapping("/info")
    @Operation(summary = "App info", description = "Get application information")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("app_name", "Hokori Web");
        response.put("description", "Japanese Learning Platform");
        response.put("features", new String[]{"Content-Based Learning", "JLPT Mock Tests"});
        response.put("deployment", "Railway");
        response.put("database", "H2 (Development)");
        return response;
    }
}
