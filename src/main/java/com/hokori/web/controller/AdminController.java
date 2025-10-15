package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.RoleAssignmentRequest;
import com.hokori.web.dto.UserStatusRequest;
import com.hokori.web.dto.RoleCreateRequest;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.RoleService;
import com.hokori.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Management", description = "Administrative functions for user and role management")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private RoleService roleService;
    
    @Autowired
    private CurrentUserService currentUserService;

    // =================================================================================
    // ROLE MANAGEMENT
    // =================================================================================

    @GetMapping("/roles")
    @Operation(summary = "Get all roles", description = "Retrieve all available roles in the system")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        try {
            List<Role> roles = roleService.getAllRoles();
            return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve roles: " + e.getMessage()));
        }
    }

    @GetMapping("/roles/{roleName}")
    @Operation(summary = "Get role by name", description = "Retrieve a specific role by its name")
    public ResponseEntity<ApiResponse<Role>> getRoleByName(@PathVariable String roleName) {
        try {
            Role role = roleService.getRoleByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            return ResponseEntity.ok(ApiResponse.success("Role retrieved successfully", role));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve role: " + e.getMessage()));
        }
    }

    @PostMapping("/roles")
    @Operation(summary = "Create new role", description = "Create a new role in the system")
    public ResponseEntity<ApiResponse<Role>> createRole(@Valid @RequestBody RoleCreateRequest request) {
        try {
            Role role = roleService.createRole(request.getRoleName(), request.getDescription());
            return ResponseEntity.ok(ApiResponse.success("Role created successfully", role));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to create role: " + e.getMessage()));
        }
    }

    // =================================================================================
    // USER MANAGEMENT
    // =================================================================================

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Retrieve all users in the system")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve user: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Assign role to user", description = "Assign a specific role to a user")
    public ResponseEntity<ApiResponse<User>> assignRoleToUser(
            @PathVariable Long userId, 
            @Valid @RequestBody RoleAssignmentRequest request) {
        try {
            // Get user and role
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Role role = roleService.getRoleByName(request.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            
            // Assign role
            user.setRoleId(role.getId());
            userService.updateUser(user);
            
            return ResponseEntity.ok(ApiResponse.success("Role assigned successfully", user));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to assign role: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Update user status", description = "Activate or deactivate a user account")
    public ResponseEntity<ApiResponse<User>> updateUserStatus(
            @PathVariable Long userId, 
            @Valid @RequestBody UserStatusRequest request) {
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setIsActive(request.getIsActive());
            userService.updateUser(user);
            
            return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to update user status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Permanently delete a user account")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", "User with ID " + userId + " has been deleted"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    @GetMapping("/users/search")
    @Operation(summary = "Search users", description = "Search users by various criteria")
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String jlptLevel,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isVerified) {
        try {
            List<User> allUsers = userService.getAllUsers();
            
            // Apply filters
            List<User> filteredUsers = allUsers.stream()
                .filter(user -> username == null || user.getUsername().toLowerCase().contains(username.toLowerCase()))
                .filter(user -> email == null || user.getEmail().toLowerCase().contains(email.toLowerCase()))
                .filter(user -> role == null || (user.getRole() != null && user.getRole().getRoleName().equals(role)))
                .filter(user -> country == null || (user.getCountry() != null && user.getCountry().toLowerCase().contains(country.toLowerCase())))
                .filter(user -> jlptLevel == null || (user.getCurrentJlptLevel() != null && user.getCurrentJlptLevel().name().equals(jlptLevel)))
                .filter(user -> isActive == null || user.getIsActive().equals(isActive))
                .filter(user -> isVerified == null || user.getIsVerified().equals(isVerified))
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Users found: " + filteredUsers.size(), filteredUsers));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to search users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/analytics")
    @Operation(summary = "Get user analytics", description = "Get detailed user analytics and insights")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserAnalytics() {
        try {
            List<User> allUsers = userService.getAllUsers();
            
            // Registration trends (last 30 days)
            Map<String, Long> registrationTrends = new HashMap<>();
            for (int i = 29; i >= 0; i--) {
                java.time.LocalDateTime date = java.time.LocalDateTime.now().minusDays(i);
                long count = allUsers.stream()
                    .filter(user -> user.getCreatedAt() != null && 
                        user.getCreatedAt().toLocalDate().equals(date.toLocalDate()))
                    .count();
                registrationTrends.put(date.toLocalDate().toString(), count);
            }
            
            // User activity by hour (last 24 hours)
            Map<String, Long> activityByHour = new HashMap<>();
            for (int i = 23; i >= 0; i--) {
                java.time.LocalDateTime hour = java.time.LocalDateTime.now().minusHours(i);
                long count = allUsers.stream()
                    .filter(user -> user.getLastLoginAt() != null && 
                        user.getLastLoginAt().isAfter(hour.minusHours(1)) && 
                        user.getLastLoginAt().isBefore(hour.plusHours(1)))
                    .count();
                activityByHour.put(hour.getHour() + ":00", count);
            }
            
            // Top countries
            Map<String, Long> topCountries = allUsers.stream()
                .filter(user -> user.getCountry() != null && !user.getCountry().isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(
                    User::getCountry,
                    java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
            
            // JLPT level distribution
            Map<String, Long> jlptDistribution = new HashMap<>();
            for (User.JLPTLevel level : User.JLPTLevel.values()) {
                long count = allUsers.stream()
                    .filter(user -> user.getCurrentJlptLevel() == level)
                    .count();
                jlptDistribution.put(level.name(), count);
            }
            
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("registrationTrends", registrationTrends);
            analytics.put("activityByHour", activityByHour);
            analytics.put("topCountries", topCountries);
            analytics.put("jlptDistribution", jlptDistribution);
            analytics.put("totalUsers", allUsers.size());
            analytics.put("activeUsers", allUsers.stream().filter(User::getIsActive).count());
            analytics.put("verifiedUsers", allUsers.stream().filter(User::getIsVerified).count());
            analytics.put("lastUpdated", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success("User analytics retrieved successfully", analytics));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve user analytics: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/verify")
    @Operation(summary = "Verify user email", description = "Manually verify a user's email address")
    public ResponseEntity<ApiResponse<User>> verifyUser(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setIsVerified(true);
            userService.updateUser(user);
            
            return ResponseEntity.ok(ApiResponse.success("User verified successfully", user));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to verify user: " + e.getMessage()));
        }
    }

    @GetMapping("/users/export")
    @Operation(summary = "Export users data", description = "Export users data in CSV format")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportUsers() {
        try {
            List<User> allUsers = userService.getAllUsers();
            
            // Create CSV-like data structure
            List<Map<String, Object>> csvData = allUsers.stream()
                .map(user -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", user.getId());
                    row.put("username", user.getUsername());
                    row.put("email", user.getEmail());
                    row.put("displayName", user.getDisplayName());
                    row.put("country", user.getCountry());
                    row.put("nativeLanguage", user.getNativeLanguage());
                    row.put("learningLanguage", user.getLearningLanguage());
                    row.put("currentJlptLevel", user.getCurrentJlptLevel() != null ? user.getCurrentJlptLevel().name() : "N/A");
                    row.put("role", user.getRole() != null ? user.getRole().getRoleName() : "No role");
                    row.put("isActive", user.getIsActive());
                    row.put("isVerified", user.getIsVerified());
                    row.put("createdAt", user.getCreatedAt());
                    row.put("lastLoginAt", user.getLastLoginAt());
                    return row;
                })
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> export = new HashMap<>();
            export.put("data", csvData);
            export.put("totalRecords", csvData.size());
            export.put("exportedAt", java.time.LocalDateTime.now());
            export.put("format", "CSV-ready");
            
            return ResponseEntity.ok(ApiResponse.success("Users data exported successfully", export));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to export users data: " + e.getMessage()));
        }
    }

    // =================================================================================
    // SYSTEM MANAGEMENT
    // =================================================================================

    @PostMapping("/initialize-roles")
    @Operation(summary = "Initialize default roles", description = "Create default roles if they don't exist")
    public ResponseEntity<ApiResponse<String>> initializeRoles() {
        try {
            roleService.initializeDefaultRoles();
            return ResponseEntity.ok(ApiResponse.success("Default roles initialized successfully", "All default roles have been created"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to initialize roles: " + e.getMessage()));
        }
    }

    @GetMapping("/system/health")
    @Operation(summary = "Get system health", description = "Get system health status and metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Database health
            try {
                List<User> users = userService.getAllUsers();
                List<Role> roles = roleService.getAllRoles();
                health.put("database", Map.of(
                    "status", "UP",
                    "usersCount", users.size(),
                    "rolesCount", roles.size()
                ));
            } catch (Exception e) {
                health.put("database", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
                ));
            }
            
            // Memory usage
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            health.put("memory", Map.of(
                "total", totalMemory,
                "used", usedMemory,
                "free", freeMemory,
                "usagePercentage", Math.round((double) usedMemory / totalMemory * 100)
            ));
            
            // System info
            health.put("system", Map.of(
                "javaVersion", System.getProperty("java.version"),
                "osName", System.getProperty("os.name"),
                "osVersion", System.getProperty("os.version"),
                "uptime", System.currentTimeMillis() - java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime()
            ));
            
            health.put("timestamp", java.time.LocalDateTime.now());
            health.put("overallStatus", "UP");
            
            return ResponseEntity.ok(ApiResponse.success("System health retrieved successfully", health));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve system health: " + e.getMessage()));
        }
    }

    @GetMapping("/system/logs")
    @Operation(summary = "Get system logs", description = "Get recent system activity logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemLogs() {
        try {
            Map<String, Object> logs = new HashMap<>();
            
            // Recent user registrations
            List<User> recentUsers = userService.getAllUsers().stream()
                .filter(user -> user.getCreatedAt() != null)
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
            
            // Recent logins
            List<User> recentLogins = userService.getAllUsers().stream()
                .filter(user -> user.getLastLoginAt() != null)
                .sorted((u1, u2) -> u2.getLastLoginAt().compareTo(u1.getLastLoginAt()))
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
            
            logs.put("recentRegistrations", recentUsers);
            logs.put("recentLogins", recentLogins);
            logs.put("totalRegistrations", userService.getAllUsers().size());
            logs.put("activeUsers", userService.getAllUsers().stream().filter(User::getIsActive).count());
            logs.put("lastUpdated", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success("System logs retrieved successfully", logs));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve system logs: " + e.getMessage()));
        }
    }

    @PostMapping("/system/cleanup")
    @Operation(summary = "System cleanup", description = "Perform system cleanup operations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performCleanup() {
        try {
            Map<String, Object> cleanup = new HashMap<>();
            
            // Count inactive users
            long inactiveUsers = userService.getAllUsers().stream()
                .filter(user -> !user.getIsActive())
                .count();
            
            // Count unverified users older than 30 days
            long oldUnverifiedUsers = userService.getAllUsers().stream()
                .filter(user -> !user.getIsVerified() && 
                    user.getCreatedAt() != null && 
                    user.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusDays(30)))
                .count();
            
            cleanup.put("inactiveUsers", inactiveUsers);
            cleanup.put("oldUnverifiedUsers", oldUnverifiedUsers);
            cleanup.put("cleanupPerformed", java.time.LocalDateTime.now());
            cleanup.put("recommendations", List.of(
                "Consider deactivating old unverified accounts",
                "Review inactive user accounts",
                "Archive old system logs"
            ));
            
            return ResponseEntity.ok(ApiResponse.success("System cleanup analysis completed", cleanup));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to perform system cleanup: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard data", description = "Get comprehensive dashboard data for admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        try {
            // Get user statistics
            List<User> allUsers = userService.getAllUsers();
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream().filter(User::getIsActive).count();
            long verifiedUsers = allUsers.stream().filter(User::getIsVerified).count();
            
            // Get role statistics
            List<Role> allRoles = roleService.getAllRoles();
            Map<String, Long> roleStats = new HashMap<>();
            for (Role role : allRoles) {
                long count = allUsers.stream()
                    .filter(user -> user.getRole() != null && user.getRole().getRoleName().equals(role.getRoleName()))
                    .count();
                roleStats.put(role.getRoleName(), count);
            }
            
            // Get recent users (last 7 days)
            List<User> recentUsers = allUsers.stream()
                .filter(user -> user.getCreatedAt() != null && 
                    user.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(7)))
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
            
            // Get users by JLPT level
            Map<String, Long> jlptStats = new HashMap<>();
            for (User.JLPTLevel level : User.JLPTLevel.values()) {
                long count = allUsers.stream()
                    .filter(user -> user.getCurrentJlptLevel() == level)
                    .count();
                jlptStats.put(level.name(), count);
            }
            
            // Get users by country
            Map<String, Long> countryStats = allUsers.stream()
                .filter(user -> user.getCountry() != null && !user.getCountry().isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(
                    User::getCountry,
                    java.util.stream.Collectors.counting()
                ));
            
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("overview", Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "verifiedUsers", verifiedUsers,
                "inactiveUsers", totalUsers - activeUsers,
                "unverifiedUsers", totalUsers - verifiedUsers,
                "totalRoles", allRoles.size()
            ));
            dashboard.put("roleDistribution", roleStats);
            dashboard.put("jlptDistribution", jlptStats);
            dashboard.put("countryDistribution", countryStats);
            dashboard.put("recentUsers", recentUsers);
            dashboard.put("lastUpdated", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully", dashboard));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve dashboard data: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get system statistics", description = "Retrieve system statistics and user counts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userService.getAllUsers().size());
            stats.put("activeUsers", userService.getAllUsers().stream().mapToInt(u -> u.getIsActive() ? 1 : 0).sum());
            stats.put("totalRoles", roleService.getAllRoles().size());
            
            // Get current user info safely
            try {
                User currentUser = currentUserService.getCurrentUser()
                        .orElseThrow(() -> new RuntimeException("Not authenticated"));
                stats.put("currentUser", currentUser.getDisplayName());
                stats.put("currentUserRole", currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "No role");
            } catch (Exception e) {
                stats.put("currentUser", "Not authenticated");
                stats.put("currentUserRole", "No role");
            }
            
            return ResponseEntity.ok(ApiResponse.success("System statistics retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve system statistics: " + e.getMessage()));
        }
    }
}
