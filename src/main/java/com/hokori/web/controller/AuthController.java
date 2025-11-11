package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.auth.AuthResponse;
import com.hokori.web.dto.auth.LoginRequest;
import com.hokori.web.dto.auth.FirebaseAuthRequest;
import com.hokori.web.dto.auth.RegisterRequest;
import com.hokori.web.dto.auth.RegisterLearnerRequest;
import com.hokori.web.dto.auth.RegisterTeacherRequest;
import com.hokori.web.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
// (Không đổi import)
import org.springframework.http.HttpStatus; // [CHANGED]
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ===========================
    // NEW: Register Learner
    // ===========================
    @PostMapping("/register/learner")
    @Operation(summary = "Register learner", description = "Đăng ký tài khoản người học (LEARNER)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerLearner(
            @Valid @RequestBody RegisterLearnerRequest req) {
        try {
            AuthResponse authResponse = authService.registerLearner(req);

            Map<String, Object> response = new HashMap<>();
            response.put("user", authResponse.getUser());
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("message", "Registration successful");
            response.put("role", "LEARNER");

            return ResponseEntity.status(HttpStatus.CREATED) // [CHANGED]
                    .body(ApiResponse.success("User registered successfully", response));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Registration failed"; // [CHANGED]
            if (msg.contains("Email already exists") || msg.contains("Username already exists")) { // [CHANGED]
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(msg)); // [CHANGED]
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                    .body(ApiResponse.error("Registration failed: " + msg)); // [CHANGED]
        }
    }

    // ===========================
    // NEW: Register Teacher
    // ===========================
    @PostMapping("/register/teacher")
    @Operation(summary = "Register teacher",
            description = "Đăng ký tài khoản giáo viên (TEACHER); hồ sơ được lưu trực tiếp trong bảng Users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerTeacher(
            @Valid @RequestBody RegisterTeacherRequest req) {
        try {
            AuthResponse authResponse = authService.registerTeacher(req);

            Map<String, Object> response = new HashMap<>();
            response.put("user", authResponse.getUser());
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("message", "Registration successful");
            response.put("role", "TEACHER");

            return ResponseEntity.status(HttpStatus.CREATED) // [CHANGED]
                    .body(ApiResponse.success("User registered successfully", response));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Registration failed"; // [CHANGED]
            if (msg.contains("Email already exists") || msg.contains("Username already exists")) { // [CHANGED]
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(msg)); // [CHANGED]
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                    .body(ApiResponse.error("Registration failed: " + msg)); // [CHANGED]
        }
    }

    // ===========================
    // BACKWARD-COMPATIBLE (OLD)
    // ===========================
    @Deprecated
    @PostMapping("/register")
    @Operation(summary = "Register new user (deprecated)",
            description = "Vui lòng dùng /register/learner hoặc /register/teacher")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody RegisterRequest registerRequest) {
        try {
            if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                        .body(ApiResponse.error("Username is required"));
            }
            if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                        .body(ApiResponse.error("Email is required"));
            }
            if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                        .body(ApiResponse.error("Password is required"));
            }
            if (registerRequest.getConfirmPassword() == null || registerRequest.getConfirmPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                        .body(ApiResponse.error("Password confirmation is required"));
            }
            if (!registerRequest.isPasswordConfirmed()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                        .body(ApiResponse.error("Password and confirmation do not match"));
            }
            if (!registerRequest.isValidRole()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                        .body(ApiResponse.error("Invalid role selected. Available roles: LEARNER, TEACHER, STAFF, ADMIN"));
            }

            // Delegate sang 2 luồng mới nếu là learner/teacher
            String role = registerRequest.getRoleName();
            if ("LEARNER".equalsIgnoreCase(role)) {
                RegisterLearnerRequest req = new RegisterLearnerRequest();
                req.setUsername(registerRequest.getUsername());
                req.setEmail(registerRequest.getEmail());
                req.setPassword(registerRequest.getPassword());
                req.setDisplayName(registerRequest.getDisplayName());
                req.setCountry(registerRequest.getCountry());
                req.setNativeLanguage(registerRequest.getNativeLanguage());
                req.setCurrentJlptLevel(registerRequest.getCurrentJlptLevel());
                return registerLearner(req);
            }
            if ("TEACHER".equalsIgnoreCase(role)) {
                RegisterTeacherRequest req = new RegisterTeacherRequest();
                req.setUsername(registerRequest.getUsername());
                req.setEmail(registerRequest.getEmail());
                req.setPassword(registerRequest.getPassword());

                String display = (registerRequest.getDisplayName() != null &&
                        !registerRequest.getDisplayName().isBlank())
                        ? registerRequest.getDisplayName()
                        : registerRequest.getUsername();

                req.setFirstName(display);
                req.setLastName("");
                req.setHeadline("Teacher");
                req.setCurrentJlptLevel(registerRequest.getCurrentJlptLevel());

                return registerTeacher(req);
            }

            // các role khác (STAFF/ADMIN) dùng luồng cũ
            AuthResponse authResponse = authService.registerUser(registerRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("user", authResponse.getUser());
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("message", "Registration successful");
            response.put("role", registerRequest.getRoleName());

            return ResponseEntity.status(HttpStatus.CREATED) // [CHANGED]
                    .body(ApiResponse.success("User registered successfully", response));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Registration failed"; // [CHANGED]
            if (msg.contains("Email already exists") || msg.contains("Username already exists")) { // [CHANGED]
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(msg)); // [CHANGED]
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) // [CHANGED]
                    .body(ApiResponse.error("Registration failed: " + msg)); // [CHANGED]
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with username/email and password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse authResponse = authService.authenticateWithUsernamePassword(loginRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("user", authResponse.getUser());
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("message", "Login successful");
            response.put("roles", authResponse.getRoles());
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Login failed"; // [CHANGED]
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // [CHANGED]
                    .body(ApiResponse.error("Login failed: " + msg)); // [CHANGED]
        }
    }

    @PostMapping("/firebase")
    @Operation(summary = "Firebase authentication", description = "Authenticate user with Firebase token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> firebaseAuth(
            @Valid @RequestBody FirebaseAuthRequest firebaseRequest) {
        try {
            AuthResponse authResponse = authService.authenticateUser(firebaseRequest.getFirebaseToken());
            Map<String, Object> response = new HashMap<>();
            response.put("user", authResponse.getUser());
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("message", "Firebase authentication successful");
            response.put("roles", authResponse.getRoles());
            return ResponseEntity.ok(ApiResponse.success("Firebase authentication successful", response));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Firebase authentication failed"; // [CHANGED]
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // [CHANGED]
                    .body(ApiResponse.error(msg)); // [CHANGED]
        }
    }

    // ===========================
    // NEW: Firebase Register (Google Sign-Up)
    // ===========================
    @PostMapping("/firebase/register") // [ADDED]
    @Operation(summary = "Firebase register", description = "Register user with Firebase token (Google Sign-Up)") // [ADDED]
    public ResponseEntity<ApiResponse<Map<String, Object>>> firebaseRegister( // [ADDED]
                                                                              @Valid @RequestBody FirebaseAuthRequest firebaseRequest) { // [ADDED]
        try {
            AuthResponse authResponse = authService.authenticateUserForRegistration(firebaseRequest.getFirebaseToken()); // [ADDED]
            Map<String, Object> response = new HashMap<>(); // [ADDED]
            response.put("user", authResponse.getUser()); // [ADDED]
            response.put("accessToken", authResponse.getAccessToken()); // [ADDED]
            response.put("refreshToken", authResponse.getRefreshToken()); // [ADDED]
            response.put("message", "Registration successful"); // [ADDED]
            response.put("roles", authResponse.getRoles()); // [ADDED]
            return ResponseEntity.status(HttpStatus.CREATED) // [ADDED]
                    .body(ApiResponse.success("Firebase registration successful", response)); // [ADDED]
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Firebase registration failed";
            if (msg.contains("EMAIL_ALREADY_EXISTS")) { // [CHANGED]
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Email already exists"));
            }
            if (msg.contains("GOOGLE_EMAIL_REQUIRED")) { // [CHANGED]
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Google account has no email"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg));
        }
    }

    @GetMapping("/roles")
    @Operation(summary = "Get available roles", description = "Get list of available roles for registration")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableRoles() {
        try {
            List<Map<String, Object>> roles = List.of(
                    Map.of("roleName", "LEARNER", "description", "Regular student/learner"),
                    Map.of("roleName", "TEACHER", "description", "Teacher who can create content"),
                    Map.of("roleName", "STAFF", "description", "Staff member with limited admin access"),
                    Map.of("roleName", "ADMIN", "description", "Full system administrator")
            );
            return ResponseEntity.ok(ApiResponse.success("Available roles retrieved successfully", roles));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve roles: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Current user endpoint - requires authentication");
            response.put("note", "Use JWT token in Authorization header");
            return ResponseEntity.ok(ApiResponse.success("Current user endpoint", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to get current user: " + e.getMessage()));
        }
    }
}
