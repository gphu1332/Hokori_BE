package com.hokori.web.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.hokori.web.config.JwtConfig;
import com.hokori.web.dto.AuthResponse;
import com.hokori.web.dto.LoginRequest;
import com.hokori.web.dto.RegisterRequest;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.RoleRepository;
import com.hokori.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    @Autowired(required = false)
    private FirebaseAuth firebaseAuth;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:3600}")
    private Long jwtExpiration;

    /* ===================== FIREBASE LOGIN ===================== */
    public AuthResponse authenticateUser(String idToken) throws FirebaseAuthException {
        if (firebaseAuth == null) {
            throw new RuntimeException("Firebase authentication is not configured. Please set firebase.enabled=true in application.properties");
        }

        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
        String firebaseUid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String displayName = decodedToken.getName();
        String photoUrl = decodedToken.getPicture();

        User user = findOrCreateUser(firebaseUid, email, displayName, photoUrl);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return createAuthResponse(user, "firebase");
    }

    /* ===================== REFRESH TOKEN ===================== */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            if (!jwtConfig.isRefreshToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            String email = jwtConfig.extractUsername(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (Boolean.FALSE.equals(user.getIsActive())) {
                throw new RuntimeException("User account is deactivated");
            }

            return createAuthResponse(user, "refresh");
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    /* ===================== FIND OR CREATE USER (FIREBASE) ===================== */
    public User findOrCreateUser(String firebaseUid, String email, String displayName, String photoUrl) {
        // 1) Tồn tại theo Firebase UID
        Optional<User> byFirebase = userRepository.findByFirebaseUid(firebaseUid);
        if (byFirebase.isPresent()) {
            User user = byFirebase.get();
            boolean updated = false;

            if (displayName != null && !displayName.equals(user.getDisplayName())) { user.setDisplayName(displayName); updated = true; }
            if (photoUrl    != null && !photoUrl.equals(user.getAvatarUrl()))      { user.setAvatarUrl(photoUrl);    updated = true; }
            if (email       != null && !email.equals(user.getEmail()))             { user.setEmail(email);           updated = true; }

            if (updated) userRepository.save(user);
            return user;
        }

        // 2) Tồn tại theo email (đã đăng ký username/password trước đó)
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setFirebaseUid(firebaseUid);

            boolean updated = false;
            if (displayName != null && !displayName.equals(user.getDisplayName())) { user.setDisplayName(displayName); updated = true; }
            if (photoUrl    != null && !photoUrl.equals(user.getAvatarUrl()))      { user.setAvatarUrl(photoUrl);    updated = true; }
            if (!Boolean.TRUE.equals(user.getIsVerified())) { user.setIsVerified(true); updated = true; }

            // nếu chưa có role thì gán mặc định
            if (user.getRole() == null) {
                user.setRole(getDefaultRole());
                updated = true;
            }
            if (updated) userRepository.save(user);
            return user;
        }

        // 3) Tạo mới + gán role mặc định TRƯỚC KHI SAVE để Hibernate set luôn FK role_id
        User newUser = new User();
        newUser.setFirebaseUid(firebaseUid);
        newUser.setEmail(email);
        newUser.setDisplayName(displayName);
        newUser.setAvatarUrl(photoUrl);
        newUser.setIsActive(true);
        newUser.setIsVerified(true);
        newUser.setRole(getDefaultRole()); // <-- quan trọng: set Role entity, KHÔNG set roleId

        return userRepository.save(newUser);
    }

    /* ===================== ROLE HELPERS ===================== */
    private Role getDefaultRole() {
        return roleRepository.findByRoleName("LEARNER")
                .orElseThrow(() -> new RuntimeException("Default role LEARNER not found"));
    }

    /** Gán role theo tên (dùng entity quan hệ) */
    public void assignRoleToUser(User user, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        user.setRole(role);
        userRepository.save(user);
    }

    public void assignRole(User user, String roleName) {
        assignRoleToUser(user, roleName);
    }

    public void removeRole(User user) {
        user.setRole(null);
        userRepository.save(user);
    }

    public boolean userHasRole(User user, String roleName) {
        return user.getRole() != null && roleName.equals(user.getRole().getRoleName());
    }

    public boolean isAdmin(User user) {
        return userHasRole(user, "ADMIN");
    }

    public boolean isStaffOrAdmin(User user) {
        return userHasRole(user, "STAFF") || userHasRole(user, "ADMIN");
    }

    public boolean canCreateContent(User user) {
        return userHasRole(user, "TEACHER") || userHasRole(user, "STAFF") || userHasRole(user, "ADMIN");
    }

    public List<String> getUserRoles(User user) {
        return (user.getRole() != null) ? List.of(user.getRole().getRoleName()) : List.of();
    }

    /* ===================== JWT HELPERS ===================== */
    public boolean validateToken(String token) {
        try {
            String email = jwtConfig.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return jwtConfig.validateToken(token, user.getEmail()) && Boolean.TRUE.equals(user.getIsActive());
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return jwtConfig.extractUsername(token);
    }

    private AuthResponse createAuthResponse(User user, String loginType) {
        List<String> roles = getUserRoles(user);
        String accessToken = jwtTokenService.generateAccessToken(user, loginType, roles);
        String refreshToken = jwtTokenService.generateRefreshToken(user);
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtTokenService.getTokenExpiration(),
                userInfo,
                roles
        );
    }

    /* ===================== USERNAME/PASSWORD LOGIN ===================== */
    public AuthResponse authenticateWithUsernamePassword(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseGet(() -> userRepository.findByEmail(loginRequest.getUsername())
                        .orElseThrow(() -> new RuntimeException("Invalid username/email or password")));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("User account is deactivated");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid username/email or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return createAuthResponse(user, "password");
    }

    public AuthResponse registerUser(RegisterRequest registerRequest) {
        if (!registerRequest.isPasswordConfirmed()) {
            throw new RuntimeException("Password and confirmation do not match");
        }
        if (!registerRequest.isValidRole()) {
            throw new RuntimeException("Invalid role selected");
        }
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setDisplayName(
                registerRequest.getDisplayName() != null ? registerRequest.getDisplayName() : registerRequest.getUsername());
        newUser.setCountry(registerRequest.getCountry());
        newUser.setNativeLanguage(registerRequest.getNativeLanguage());

        // tạo uid giả cho user/password
        String usernamePasswordUid = "username_" + registerRequest.getUsername() + "_" + System.currentTimeMillis();
        newUser.setFirebaseUid(usernamePasswordUid);

        // JLPT
        if (registerRequest.getCurrentJlptLevel() != null) {
            try {
                newUser.setCurrentJlptLevel(User.JLPTLevel.valueOf(registerRequest.getCurrentJlptLevel()));
            } catch (IllegalArgumentException e) {
                newUser.setCurrentJlptLevel(User.JLPTLevel.N5);
            }
        } else {
            newUser.setCurrentJlptLevel(User.JLPTLevel.N5);
        }

        newUser.setIsActive(true);
        newUser.setIsVerified(false);
        newUser.setLearningLanguage("Japanese");

        // Gán role theo request (hoặc LEARNER) TRƯỚC khi save để set FK
        String roleName = (registerRequest.getRoleName() != null) ? registerRequest.getRoleName() : "LEARNER";
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        newUser.setRole(role);

        newUser = userRepository.save(newUser);

        return createAuthResponse(newUser, "registration");
    }

    /* ===================== SIMPLE GETTERS ===================== */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
