package com.hokori.web.controller;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.Enum.WalletTransactionSource;
import com.hokori.web.Enum.WalletTransactionStatus;
import com.hokori.web.constants.RoleConstants;
import com.hokori.web.dto.*;
import com.hokori.web.entity.Payment;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.*;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.RoleService;
import com.hokori.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Controller.
 * NOTE: @PreAuthorize uses string literal (Spring Security requirement).
 * Role name reference: RoleConstants.ADMIN
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Management", description = "Administrative functions for user and role management")
@SecurityRequirement(name = "Bearer Authentication") // chỉ dành cho Swagger UI
@CrossOrigin(origins = "*")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')") // ⬅️ BẮT BUỘC LÀ ADMIN (RoleConstants.ADMIN)
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

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
    public ResponseEntity<ApiResponse<List<UserDetailDTO>>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserDetailDTO> userDTOs = users.stream()
                    .map(UserDetailDTO::from)
                    .toList();
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", userDTOs));
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
    public ResponseEntity<ApiResponse<UserDetailDTO>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusRequest request) {
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setIsActive(request.getIsActive());
            userService.updateUser(user);

            // Load user with role for DTO conversion
            User updated = userService.getUserWithRole(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(ApiResponse.success("User status updated successfully", UserDetailDTO.from(updated)));
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
    public ResponseEntity<ApiResponse<List<UserDetailDTO>>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String jlptLevel,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isVerified) {
        try {
            List<User> allUsers = userService.getAllUsers();

            List<UserDetailDTO> filteredUsers = allUsers.stream()
                    .filter(u -> username == null || (u.getUsername() != null && u.getUsername().toLowerCase().contains(username.toLowerCase())))
                    .filter(u -> email == null || (u.getEmail() != null && u.getEmail().toLowerCase().contains(email.toLowerCase())))
                    .filter(u -> role == null || (u.getRole() != null && role.equals(u.getRole().getRoleName())))
                    .filter(u -> jlptLevel == null || (u.getCurrentJlptLevel() != null && u.getCurrentJlptLevel().name().equals(jlptLevel)))
                    .filter(u -> isActive == null || Boolean.valueOf(Boolean.TRUE.equals(u.getIsActive())).equals(isActive))
                    .filter(u -> isVerified == null || Boolean.valueOf(Boolean.TRUE.equals(u.getIsVerified())).equals(isVerified))
                    .map(UserDetailDTO::from)
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
    public ResponseEntity<ApiResponse<UserDetailDTO>> verifyUser(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setIsVerified(true);
            userService.updateUser(user);

            // Load user with role for DTO conversion
            User updated = userService.getUserWithRole(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(ApiResponse.success("User verified successfully", UserDetailDTO.from(updated)));
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

            // Convert to DTO first to avoid LOB serialization issues
            List<UserDetailDTO> userDTOs = allUsers.stream()
                    .map(UserDetailDTO::from)
                    .toList();

            List<Map<String, Object>> csvData = userDTOs.stream()
                    .map(u -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", u.getId());
                        row.put("username", u.getUsername());
                        row.put("email", u.getEmail());
                        row.put("displayName", u.getDisplayName());
                        row.put("currentJlptLevel", u.getCurrentJlptLevel() != null ? u.getCurrentJlptLevel().name() : "N/A");
                        row.put("role", u.getRoleName() != null ? u.getRoleName() : "No role");
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

    /**
     * TEMPORARY: Fix course_status_check constraint to allow all CourseStatus values
     * TODO: Remove this endpoint after fixing the constraint via migration
     */
    @PostMapping("/database/fix-course-status-constraint")
    @Operation(summary = "[TEMPORARY] Fix course status constraint", 
               description = "Fix course_status_check constraint to allow all CourseStatus values including REJECTED and FLAGGED. Remove after use.")
    public ResponseEntity<ApiResponse<String>> fixCourseStatusConstraint() {
        try {
            // Drop existing constraint if exists
            jdbcTemplate.execute("ALTER TABLE course DROP CONSTRAINT IF EXISTS course_status_check");
            
            // Add new constraint with all CourseStatus values
            jdbcTemplate.execute(
                "ALTER TABLE course " +
                "ADD CONSTRAINT course_status_check " +
                "CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'PUBLISHED', 'FLAGGED', 'ARCHIVED'))"
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Course status constraint fixed successfully", 
                "Constraint now allows: DRAFT, PENDING_APPROVAL, REJECTED, PUBLISHED, FLAGGED, ARCHIVED"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to fix constraint: " + e.getMessage())
            );
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

            // Recent user registrations - convert to DTO
            List<UserDetailDTO> recentUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null)
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .limit(20)
                    .map(UserDetailDTO::from)
                    .toList();

            // Recent logins - convert to DTO
            List<UserDetailDTO> recentLogins = allUsers.stream()
                    .filter(u -> u.getLastLoginAt() != null)
                    .sorted((u1, u2) -> u2.getLastLoginAt().compareTo(u1.getLastLoginAt()))
                    .limit(20)
                    .map(UserDetailDTO::from)
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

            // Get recent users (last 7 days) - convert to DTO to avoid serialization issues
            List<UserSimpleDTO> recentUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null
                            && u.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(7)))
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .limit(10)
                    .map(UserSimpleDTO::from)
                    .toList();


            Map<String, Long> jlptStats = new HashMap<>();
            for (JLPTLevel level : JLPTLevel.values()) {
                long count = allUsers.stream()
                        .filter(u -> u.getCurrentJlptLevel() == level)
                        .count();
                jlptStats.put(level.name(), count);
            }


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

    // =================================================================================
    // TEACHER MANAGEMENT & STATISTICS
    // =================================================================================

    @GetMapping("/teachers/{teacherId}")
    @Operation(summary = "Get teacher details with contributions", 
               description = "Get detailed information about a teacher including courses, statistics, and revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeacherDetails(@PathVariable Long teacherId) {
        try {
            User teacher = userService.getUserWithRole(teacherId)
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            // Verify user is a teacher
            if (teacher.getRole() == null || !RoleConstants.TEACHER.equalsIgnoreCase(teacher.getRole().getRoleName())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a teacher"));
            }

            // Get teacher courses
            List<Object[]> courseMetadataList = courseRepository.findCourseMetadataByUserId(teacherId, null, null);
            List<Map<String, Object>> courses = courseMetadataList.stream().map(metadata -> {
                Map<String, Object> course = new HashMap<>();
                course.put("id", metadata[0]);
                course.put("title", metadata[1]);
                course.put("slug", metadata[2]);
                course.put("status", metadata[9]);
                course.put("priceCents", metadata[5]);
                course.put("discountedPriceCents", metadata[6]);
                course.put("publishedAt", metadata[10]);
                return course;
            }).collect(Collectors.toList());

            // Count courses by status
            long publishedCourses = courseRepository.countByUserIdAndStatusAndDeletedFlagFalse(teacherId, CourseStatus.PUBLISHED);
            long draftCourses = courseRepository.countByUserIdAndStatusAndDeletedFlagFalse(teacherId, CourseStatus.DRAFT);
            long pendingCourses = courseRepository.countByUserIdAndStatusAndDeletedFlagFalse(teacherId, CourseStatus.PENDING_APPROVAL);

            // Get total enrollments - count enrollments for all courses of this teacher
            List<Long> teacherCourseIds = courseMetadataList.stream()
                    .map(metadata -> ((Number) metadata[0]).longValue())
                    .collect(Collectors.toList());
            long totalEnrollments = teacherCourseIds.stream()
                    .mapToLong(courseId -> enrollmentRepository.countByCourseId(courseId))
                    .sum();

            // Get total revenue (all time)
            List<com.hokori.web.entity.WalletTransaction> allTransactions = walletTransactionRepository
                    .findByUser_IdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt")))
                    .getContent();
            
            Long totalRevenueCents = allTransactions.stream()
                    .filter(tx -> tx.getStatus() == WalletTransactionStatus.COMPLETED 
                            && tx.getSource() == WalletTransactionSource.COURSE_SALE)
                    .mapToLong(com.hokori.web.entity.WalletTransaction::getAmountCents)
                    .sum();

            // Get monthly revenue (current month)
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
            YearMonth currentMonth = YearMonth.now(zone);
            ZonedDateTime fromZdt = currentMonth.atDay(1).atStartOfDay(zone);
            ZonedDateTime toZdt = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone);
            
            Long monthlyRevenueCents = walletTransactionRepository.sumIncomeForPeriod(
                    teacherId,
                    WalletTransactionStatus.COMPLETED,
                    WalletTransactionSource.COURSE_SALE,
                    fromZdt.toInstant(),
                    toZdt.toInstant()
            );
            if (monthlyRevenueCents == null) monthlyRevenueCents = 0L;

            Map<String, Object> teacherDetails = new HashMap<>();
            teacherDetails.put("teacher", Map.of(
                    "id", teacher.getId(),
                    "email", teacher.getEmail(),
                    "username", teacher.getUsername() != null ? teacher.getUsername() : "N/A",
                    "displayName", teacher.getDisplayName() != null ? teacher.getDisplayName() : "N/A",
                    "bio", teacher.getBio() != null ? teacher.getBio() : "",
                    "currentJlptLevel", teacher.getCurrentJlptLevel() != null ? teacher.getCurrentJlptLevel().name() : "N/A",
                    "approvalStatus", teacher.getApprovalStatus() != null ? teacher.getApprovalStatus().name() : "NONE",
                    "walletBalance", teacher.getWalletBalance() != null ? teacher.getWalletBalance() : 0L,
                    "createdAt", teacher.getCreatedAt()
            ));

            teacherDetails.put("courses", courses);
            teacherDetails.put("statistics", Map.of(
                    "totalCourses", courses.size(),
                    "publishedCourses", publishedCourses,
                    "draftCourses", draftCourses,
                    "pendingCourses", pendingCourses,
                    "totalEnrollments", totalEnrollments
            ));

            teacherDetails.put("revenue", Map.of(
                    "totalRevenueCents", totalRevenueCents,
                    "totalRevenue", BigDecimal.valueOf(totalRevenueCents).movePointLeft(2),
                    "monthlyRevenueCents", monthlyRevenueCents,
                    "monthlyRevenue", BigDecimal.valueOf(monthlyRevenueCents).movePointLeft(2),
                    "currentMonth", currentMonth.toString()
            ));

            return ResponseEntity.ok(ApiResponse.success("Teacher details retrieved successfully", teacherDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Failed to retrieve teacher details: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve teacher details: " + e.getMessage()));
        }
    }

    @GetMapping("/teachers/{teacherId}/revenue")
    @Operation(summary = "Get teacher revenue statistics", 
               description = "Get detailed revenue statistics for a teacher including total and monthly breakdown")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeacherRevenue(@PathVariable Long teacherId,
                                                                                @RequestParam(required = false) Integer year,
                                                                                @RequestParam(required = false) Integer month) {
        try {
            User teacher = userService.getUserWithRole(teacherId)
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));

            if (teacher.getRole() == null || !RoleConstants.TEACHER.equalsIgnoreCase(teacher.getRole().getRoleName())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a teacher"));
            }

            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
            YearMonth targetMonth;
            if (year != null && month != null) {
                targetMonth = YearMonth.of(year, month);
            } else {
                targetMonth = YearMonth.now(zone);
            }

            ZonedDateTime fromZdt = targetMonth.atDay(1).atStartOfDay(zone);
            ZonedDateTime toZdt = targetMonth.plusMonths(1).atDay(1).atStartOfDay(zone);

            // Get revenue for the period
            Long revenueCents = walletTransactionRepository.sumIncomeForPeriod(
                    teacherId,
                    WalletTransactionStatus.COMPLETED,
                    WalletTransactionSource.COURSE_SALE,
                    fromZdt.toInstant(),
                    toZdt.toInstant()
            );
            if (revenueCents == null) revenueCents = 0L;

            // Get all transactions for the period
            List<com.hokori.web.entity.WalletTransaction> transactions = walletTransactionRepository
                    .findByUser_IdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 1000))
                    .getContent()
                    .stream()
                    .filter(tx -> tx.getStatus() == WalletTransactionStatus.COMPLETED 
                            && tx.getSource() == WalletTransactionSource.COURSE_SALE
                            && tx.getCreatedAt().isAfter(fromZdt.toInstant())
                            && tx.getCreatedAt().isBefore(toZdt.toInstant()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> transactionDetails = transactions.stream().map(tx -> {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", tx.getId());
                detail.put("amountCents", tx.getAmountCents());
                detail.put("amount", BigDecimal.valueOf(tx.getAmountCents()).movePointLeft(2));
                detail.put("courseId", tx.getCourse() != null ? tx.getCourse().getId() : null);
                detail.put("courseTitle", tx.getCourse() != null ? tx.getCourse().getTitle() : "N/A");
                detail.put("description", tx.getDescription());
                detail.put("createdAt", tx.getCreatedAt());
                return detail;
            }).collect(Collectors.toList());

            Map<String, Object> revenue = new HashMap<>();
            revenue.put("teacherId", teacherId);
            revenue.put("teacherName", teacher.getDisplayName() != null ? teacher.getDisplayName() : teacher.getEmail());
            revenue.put("period", targetMonth.toString());
            revenue.put("revenueCents", revenueCents);
            revenue.put("revenue", BigDecimal.valueOf(revenueCents).movePointLeft(2));
            revenue.put("transactionCount", transactions.size());
            revenue.put("transactions", transactionDetails);
            revenue.put("walletBalance", teacher.getWalletBalance() != null ? teacher.getWalletBalance() : 0L);

            return ResponseEntity.ok(ApiResponse.success("Teacher revenue retrieved successfully", revenue));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Failed to retrieve teacher revenue: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve teacher revenue: " + e.getMessage()));
        }
    }

    @GetMapping("/payments")
    @Operation(summary = "Get all payments", 
               description = "Get all payment records, optionally filtered by status (PAID for successful payments)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Payment> paymentPage;

            if (status != null) {
                // Filter by status - need to add this method to repository
                List<Payment> allPayments = paymentRepository.findAll();
                List<Payment> filtered = allPayments.stream()
                        .filter(p -> p.getStatus() == status)
                        .collect(Collectors.toList());
                
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), filtered.size());
                List<Payment> pageContent = filtered.subList(start, end);
                
                paymentPage = new org.springframework.data.domain.PageImpl<>(
                        pageContent, pageable, filtered.size());
            } else {
                paymentPage = paymentRepository.findAll(pageable);
            }

            List<Map<String, Object>> paymentList = paymentPage.getContent().stream().map(payment -> {
                Map<String, Object> p = new HashMap<>();
                p.put("id", payment.getId());
                p.put("orderCode", payment.getOrderCode());
                p.put("amountCents", payment.getAmountCents());
                p.put("amount", BigDecimal.valueOf(payment.getAmountCents()).movePointLeft(2));
                p.put("status", payment.getStatus());
                p.put("userId", payment.getUserId());
                p.put("description", payment.getDescription());
                p.put("paidAt", payment.getPaidAt());
                p.put("createdAt", payment.getCreatedAt());
                
                // Parse courseIds if available
                if (payment.getCourseIds() != null && !payment.getCourseIds().isEmpty()) {
                    try {
                        // Simple parsing - assuming JSON array format [1,2,3]
                        String courseIdsStr = payment.getCourseIds().replaceAll("[\\[\\]\\s]", "");
                        if (!courseIdsStr.isEmpty()) {
                            List<Long> courseIds = new ArrayList<>();
                            for (String id : courseIdsStr.split(",")) {
                                try {
                                    courseIds.add(Long.parseLong(id.trim()));
                                } catch (NumberFormatException e) {
                                    // Skip invalid IDs
                                }
                            }
                            p.put("courseIds", courseIds);
                        }
                    } catch (Exception e) {
                        p.put("courseIds", new ArrayList<>());
                    }
                } else {
                    p.put("courseIds", new ArrayList<>());
                }
                
                p.put("aiPackageId", payment.getAiPackageId());
                return p;
            }).collect(Collectors.toList());

            // Calculate total amount for successful payments
            long totalPaidCents = paymentPage.getContent().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PAID)
                    .mapToLong(Payment::getAmountCents)
                    .sum();

            Map<String, Object> result = new HashMap<>();
            result.put("payments", paymentList);
            result.put("totalElements", paymentPage.getTotalElements());
            result.put("totalPages", paymentPage.getTotalPages());
            result.put("currentPage", paymentPage.getNumber());
            result.put("pageSize", paymentPage.getSize());
            result.put("totalPaidCents", totalPaidCents);
            result.put("totalPaidAmount", BigDecimal.valueOf(totalPaidCents).movePointLeft(2));
            result.put("filterStatus", status != null ? status.name() : "ALL");

            return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve payments: " + e.getMessage()));
        }
    }

    @GetMapping("/teachers")
    @Operation(summary = "Get all teachers", 
               description = "Get list of all teachers with basic statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllTeachers() {
        try {
            List<User> allUsers = userService.getAllUsers();
            List<User> teachers = allUsers.stream()
                    .filter(u -> u.getRole() != null && RoleConstants.TEACHER.equalsIgnoreCase(u.getRole().getRoleName()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> teacherList = teachers.stream().map(teacher -> {
                // Get basic stats
                long publishedCourses = courseRepository.countByUserIdAndStatusAndDeletedFlagFalse(
                        teacher.getId(), CourseStatus.PUBLISHED);
                // Count enrollments for all courses of this teacher
                List<Object[]> teacherCourses = courseRepository.findCourseMetadataByUserId(teacher.getId(), null, null);
                long totalEnrollments = teacherCourses.stream()
                        .mapToLong(metadata -> {
                            Long courseId = ((Number) metadata[0]).longValue();
                            return enrollmentRepository.countByCourseId(courseId);
                        })
                        .sum();

                // Get total revenue
                List<com.hokori.web.entity.WalletTransaction> transactions = walletTransactionRepository
                        .findByUser_IdOrderByCreatedAtDesc(teacher.getId(), PageRequest.of(0, 1000))
                        .getContent();
                Long totalRevenueCents = transactions.stream()
                        .filter(tx -> tx.getStatus() == WalletTransactionStatus.COMPLETED 
                                && tx.getSource() == WalletTransactionSource.COURSE_SALE)
                        .mapToLong(com.hokori.web.entity.WalletTransaction::getAmountCents)
                        .sum();

                Map<String, Object> t = new HashMap<>();
                t.put("id", teacher.getId());
                t.put("email", teacher.getEmail());
                t.put("username", teacher.getUsername());
                t.put("displayName", teacher.getDisplayName());
                t.put("approvalStatus", teacher.getApprovalStatus() != null ? teacher.getApprovalStatus().name() : "NONE");
                t.put("publishedCourses", publishedCourses);
                t.put("totalEnrollments", totalEnrollments);
                t.put("totalRevenueCents", totalRevenueCents);
                t.put("totalRevenue", BigDecimal.valueOf(totalRevenueCents).movePointLeft(2));
                t.put("walletBalance", teacher.getWalletBalance() != null ? teacher.getWalletBalance() : 0L);
                t.put("createdAt", teacher.getCreatedAt());
                return t;
            }).collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("teachers", teacherList);
            result.put("totalTeachers", teacherList.size());
            result.put("approvedTeachers", teachers.stream()
                    .filter(t -> t.getApprovalStatus() != null && 
                            t.getApprovalStatus().name().equals("APPROVED"))
                    .count());
            result.put("pendingTeachers", teachers.stream()
                    .filter(t -> t.getApprovalStatus() != null && 
                            t.getApprovalStatus().name().equals("PENDING"))
                    .count());

            return ResponseEntity.ok(ApiResponse.success("Teachers retrieved successfully", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve teachers: " + e.getMessage()));
        }
    }
}
