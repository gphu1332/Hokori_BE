package com.hokori.web.controller;

import com.hokori.web.entity.User;
import com.hokori.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User management endpoints")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve list of all users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("total", users.size());
            response.put("timestamp", LocalDateTime.now());
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve users");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<Map<String, Object>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        try {
            Optional<User> userOpt = userService.getUserById(id);
            Map<String, Object> response = new HashMap<>();
            
            if (userOpt.isPresent()) {
                response.put("user", userOpt.get());
                response.put("status", "success");
                response.put("timestamp", LocalDateTime.now());
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "User not found");
                response.put("status", "error");
                response.put("timestamp", LocalDateTime.now());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve user");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Get active users", description = "Retrieve list of active users")
    public ResponseEntity<Map<String, Object>> getActiveUsers() {
        try {
            List<User> activeUsers = userService.getActiveUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("users", activeUsers);
            response.put("total", activeUsers.size());
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve active users");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/role/{roleName}")
    @Operation(summary = "Get users by role", description = "Retrieve users with specific role")
    public ResponseEntity<Map<String, Object>> getUsersByRole(
            @Parameter(description = "Role name") @PathVariable String roleName) {
        try {
            List<User> users = userService.getUsersByRole(roleName);
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("total", users.size());
            response.put("role", roleName);
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve users by role");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivate a user account")
    public ResponseEntity<Map<String, Object>> deactivateUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        try {
            userService.deactivateUser(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deactivated successfully");
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to deactivate user");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate user", description = "Activate a user account")
    public ResponseEntity<Map<String, Object>> activateUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        try {
            userService.activateUser(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User activated successfully");
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to activate user");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Get statistics about users")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            List<User> allUsers = userService.getAllUsers();
            Map<String, Object> response = new HashMap<>();
            
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream().filter(User::getIsActive).count();
            long verifiedUsers = allUsers.stream().filter(User::getIsVerified).count();
            
            // Count by JLPT levels
            Map<String, Long> jlptStats = new HashMap<>();
            for (User.JLPTLevel level : User.JLPTLevel.values()) {
                long count = allUsers.stream()
                    .filter(u -> level.equals(u.getCurrentJlptLevel()))
                    .count();
                jlptStats.put(level.name(), count);
            }
            
            response.put("totalUsers", totalUsers);
            response.put("activeUsers", activeUsers);
            response.put("verifiedUsers", verifiedUsers);
            response.put("jlptLevelStats", jlptStats);
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to retrieve user statistics");
            errorResponse.put("status", "error");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
