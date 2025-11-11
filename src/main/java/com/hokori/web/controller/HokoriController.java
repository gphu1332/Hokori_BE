package com.hokori.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "System", description = "System health and utility endpoints")
@CrossOrigin(origins = "*")
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
    @Operation(summary = "Hello endpoint", description = "Greeting endpoint with optional name parameter")
    public Map<String, String> hello(
            @Parameter(description = "Name to greet") @RequestParam(defaultValue = "World") String name) {
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
}