package com.hokori.web.controller;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.dto.*;
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
@SecurityRequirement(name = "Bearer Authentication") // chỉ dành cho Swagger UI
@CrossOrigin(origins = "*")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')") // ⬅️ BẮT BUỘC LÀ ADMIN
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
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve roles: " + e.getMessage()));
        }
    }

    @GetMapping("/roles/{roleName}")
    @Operation(summary = "Get role by name", description = "Retrieve a specific role by its name")
    public ResponseEntity<ApiResponse<Role>> getRoleByName(@PathVariable String roleName) {
        try {
            Role role = roleService.getRoleByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            return ResponseEntity.ok(ApiResponse.success("Role retrieved successfully", role));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Failed to retrieve role: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve role: " + e.getMessage()));
        }
    }

    @PostMapping("/roles")
    @Operation(summary = "Create new role", description = "Create a new role in the system")
    public ResponseEntity<ApiResponse<Role>> createRole(@Valid @RequestBody RoleCreateRequest request) {
        try {
            Role role = roleService.createRole(request.getRoleName(), request.getDescription());
            return ResponseEntity.ok(ApiResponse.success("Role created successfully", role));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create role: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to create role: " + e.getMessage()));
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
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<ApiResponse<UserSimpleDTO>> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.getUserWithRole(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(
                    ApiResponse.success("User retrieved successfully", UserSimpleDTO.from(user))
            );
        } catch (RuntimeException e) {
            int status = (e.getMessage() != null && e.getMessage().contains("not found")) ? 404 : 400;
            return ResponseEntity.status(status).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve user: " + e.getMessage()));
        }
    }


    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Assign role to user", description = "Assign a specific role to a user")
    public ResponseEntity<ApiResponse<UserSimpleDTO>> assignRoleToUser(
            @PathVariable Long userId,
            @Valid @RequestBody RoleAssignmentRequest request) {
        try {
            // Gán role đúng chuẩn JPA (set Role entity)
            userService.assignRole(userId, request.getRoleName());

            // Load lại user với role đã fetch để tránh LazyInitializationException
            User updated = userService.getUserWithRole(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(
                    ApiResponse.success("Role assigned successfully", UserSimpleDTO.from(updated))
            );
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            int status = (msg != null && (msg.contains("User not found") || msg.contains("Role not found"))) ? 404 : 400;
            return ResponseEntity.status(status).body(ApiResponse.error("Failed to assign role: " + msg));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to assign role: " + e.getMessage()));
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
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Failed to update user status: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update user status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Permanently delete a user account")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", "User with ID " + userId + " has been deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
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

            List<User> filteredUsers = allUsers.stream()
                    .filter(u -> username == null || (u.getUsername() != null && u.getUsername().toLowerCase().contains(username.toLowerCase())))
                    .filter(u -> email == null || (u.getEmail() != null && u.getEmail().toLowerCase().contains(email.toLowerCase())))
                    .filter(u -> role == null || (u.getRole() != null && role.equals(u.getRole().getRoleName())))
                    .filter(u -> country == null || (u.getCountry() != null && u.getCountry().toLowerCase().contains(country.toLowerCase())))
                    .filter(u -> jlptLevel == null || (u.getCurrentJlptLevel() != null && u.getCurrentJlptLevel().name().equals(jlptLevel)))
                    .filter(u -> isActive == null || Boolean.valueOf(Boolean.TRUE.equals(u.getIsActive())).equals(isActive))
                    .filter(u -> isVerified == null || Boolean.valueOf(Boolean.TRUE.equals(u.getIsVerified())).equals(isVerified))
                    .toList();

            return ResponseEntity.ok(ApiResponse.success("Users found: " + filteredUsers.size(), filteredUsers));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to search users: " + e.getMessage()));
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
                        .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(date.toLocalDate()))
                        .count();
                registrationTrends.put(date.toLocalDate().toString(), count);
            }

            // User activity by hour (last 24 hours)
            Map<String, Long> activityByHour = new HashMap<>();
            for (int i = 23; i >= 0; i--) {
                java.time.LocalDateTime hour = java.time.LocalDateTime.now().minusHours(i);
                long count = allUsers.stream()
                        .filter(u -> u.getLastLoginAt() != null
                                && !u.getLastLoginAt().isBefore(hour.minusHours(1))
                                && !u.getLastLoginAt().isAfter(hour.plusHours(1)))
                        .count();
                activityByHour.put(hour.getHour() + ":00", count);
            }

            // Top countries
            Map<String, Long> topCountries = allUsers.stream()
                    .filter(u -> u.getCountry() != null && !u.getCountry().isEmpty())
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
            for (JLPTLevel level : JLPTLevel.values()) {
                long count = allUsers.stream()
                        .filter(u -> u.getCurrentJlptLevel() == level)
                        .count();
                jlptDistribution.put(level.name(), count);
            }

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("registrationTrends", registrationTrends);
            analytics.put("activityByHour", activityByHour);
            analytics.put("topCountries", topCountries);
            analytics.put("jlptDistribution", jlptDistribution);
            analytics.put("totalUsers", allUsers.size());
            analytics.put("activeUsers", allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count());   // ✅ tránh NPE
            analytics.put("verifiedUsers", allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsVerified())).count()); // ✅ tránh NPE
            analytics.put("lastUpdated", java.time.LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success("User analytics retrieved successfully", analytics));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve user analytics: " + e.getMessage()));
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
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Failed to verify user: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to verify user: " + e.getMessage()));
        }
    }

    @GetMapping("/users/export")
    @Operation(summary = "Export users data", description = "Export users data in CSV format")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportUsers() {
        try {
            List<User> allUsers = userService.getAllUsers();

            List<Map<String, Object>> csvData = allUsers.stream()
                    .map(u -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", u.getId());
                        row.put("username", u.getUsername());
                        row.put("email", u.getEmail());
                        row.put("displayName", u.getDisplayName());
                        row.put("country", u.getCountry());
                        row.put("nativeLanguage", u.getNativeLanguage());
                        row.put("learningLanguage", u.getLearningLanguage());
                        row.put("currentJlptLevel", u.getCurrentJlptLevel() != null ? u.getCurrentJlptLevel().name() : "N/A");
                        row.put("role", u.getRole() != null ? u.getRole().getRoleName() : "No role");
                        row.put("isActive", u.getIsActive());
                        row.put("isVerified", u.getIsVerified());
                        row.put("createdAt", u.getCreatedAt());
                        row.put("lastLoginAt", u.getLastLoginAt());
                        return row;
                    })
                    .toList();

            Map<String, Object> export = new HashMap<>();
            export.put("data", csvData);
            export.put("totalRecords", csvData.size());
            export.put("exportedAt", java.time.LocalDateTime.now());
            export.put("format", "CSV-ready");

            return ResponseEntity.ok(ApiResponse.success("Users data exported successfully", export));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to export users data: " + e.getMessage()));
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
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to initialize roles: " + e.getMessage()));
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
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve system health: " + e.getMessage()));
        }
    }

    @GetMapping("/system/logs")
    @Operation(summary = "Get system logs", description = "Get recent system activity logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemLogs() {
        try {
            Map<String, Object> logs = new HashMap<>();

            List<User> allUsers = userService.getAllUsers();

            // Recent user registrations
            List<User> recentUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null)
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .limit(20)
                    .toList();

            // Recent logins
            List<User> recentLogins = allUsers.stream()
                    .filter(u -> u.getLastLoginAt() != null)
                    .sorted((u1, u2) -> u2.getLastLoginAt().compareTo(u1.getLastLoginAt()))
                    .limit(20)
                    .toList();

            logs.put("recentRegistrations", recentUsers);
            logs.put("recentLogins", recentLogins);
            logs.put("totalRegistrations", allUsers.size());
            logs.put("activeUsers", allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count()); // ✅ tránh NPE
            logs.put("lastUpdated", java.time.LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success("System logs retrieved successfully", logs));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve system logs: " + e.getMessage()));
        }
    }

    @PostMapping("/system/cleanup")
    @Operation(summary = "System cleanup", description = "Perform system cleanup operations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performCleanup() {
        try {
            Map<String, Object> cleanup = new HashMap<>();

            List<User> allUsers = userService.getAllUsers();

            long inactiveUsers = allUsers.stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsActive()))
                    .count();

            long oldUnverifiedUsers = allUsers.stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsVerified())
                            && u.getCreatedAt() != null
                            && u.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusDays(30)))
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
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to perform system cleanup: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard data", description = "Get comprehensive dashboard data for admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        try {
            List<User> allUsers = userService.getAllUsers();
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();   // ✅
            long verifiedUsers = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsVerified())).count(); // ✅

            List<Role> allRoles = roleService.getAllRoles();
            Map<String, Long> roleStats = new HashMap<>();
            for (Role role : allRoles) {
                long count = allUsers.stream()
                        .filter(u -> u.getRole() != null && role.getRoleName().equals(u.getRole().getRoleName()))
                        .count();
                roleStats.put(role.getRoleName(), count);
            }

            // Get recent users (last 7 days)
            List<User> recentUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null
                            && u.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(7)))
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .limit(10)
                    .toList();


            Map<String, Long> jlptStats = new HashMap<>();
            for (JLPTLevel level : JLPTLevel.values()) {
                long count = allUsers.stream()
                        .filter(u -> u.getCurrentJlptLevel() == level)
                        .count();
                jlptStats.put(level.name(), count);
            }

            Map<String, Long> countryStats = allUsers.stream()
                    .filter(u -> u.getCountry() != null && !u.getCountry().isEmpty())
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
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve dashboard data: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get system statistics", description = "Retrieve system statistics and user counts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            List<User> allUsers = userService.getAllUsers();

            stats.put("totalUsers", allUsers.size());
            stats.put("activeUsers", allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count()); // ✅
            stats.put("totalRoles", roleService.getAllRoles().size());

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
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to retrieve system statistics: " + e.getMessage()));
        }
    }
}
