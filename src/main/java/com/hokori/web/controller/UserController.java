package com.hokori.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User management endpoints")
@CrossOrigin(origins = "*")
public class UserController {

    // Mock data for testing
    private List<Map<String, Object>> users = new ArrayList<>();

    public UserController() {
        // Initialize with sample data
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1);
        user1.put("username", "student1");
        user1.put("email", "student1@hokori.com");
        user1.put("role", "STUDENT");
        user1.put("jlptLevel", "N3");
        user1.put("createdAt", LocalDateTime.now().minusDays(30));
        users.add(user1);

        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 2);
        user2.put("username", "teacher1");
        user2.put("email", "teacher1@hokori.com");
        user2.put("role", "TEACHER");
        user2.put("specialization", "N3");
        user2.put("createdAt", LocalDateTime.now().minusDays(15));
        users.add(user2);
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve list of all users")
    public Map<String, Object> getAllUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("total", users.size());
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public Map<String, Object> getUserById(
            @Parameter(description = "User ID") @PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Map<String, Object>> user = users.stream()
                .filter(u -> u.get("id").equals(id))
                .findFirst();
        
        if (user.isPresent()) {
            response.put("user", user.get());
            response.put("status", "success");
        } else {
            response.put("message", "User not found");
            response.put("status", "error");
        }
        
        return response;
    }

    @PostMapping
    @Operation(summary = "Create new user", description = "Create a new user account")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> userData) {
        Map<String, Object> response = new HashMap<>();
        
        // Generate new ID
        int newId = users.size() + 1;
        
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("id", newId);
        newUser.put("username", userData.get("username"));
        newUser.put("email", userData.get("email"));
        newUser.put("role", userData.getOrDefault("role", "STUDENT"));
        newUser.put("createdAt", LocalDateTime.now());
        
        users.add(newUser);
        
        response.put("user", newUser);
        response.put("message", "User created successfully");
        response.put("status", "success");
        
        return response;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Get statistics about users")
    public Map<String, Object> getUserStats() {
        Map<String, Object> response = new HashMap<>();
        
        long studentCount = users.stream()
                .filter(u -> "STUDENT".equals(u.get("role")))
                .count();
        
        long teacherCount = users.stream()
                .filter(u -> "TEACHER".equals(u.get("role")))
                .count();
        
        response.put("totalUsers", users.size());
        response.put("students", studentCount);
        response.put("teachers", teacherCount);
        response.put("timestamp", LocalDateTime.now());
        
        return response;
    }
}
