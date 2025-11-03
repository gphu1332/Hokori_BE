package com.hokori.web.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.hokori.web.config.JwtConfig;
import com.hokori.web.dto.auth.AuthResponse;
import com.hokori.web.dto.auth.LoginRequest;
import com.hokori.web.dto.auth.RegisterLearnerRequest;
import com.hokori.web.dto.auth.RegisterRequest;
import com.hokori.web.dto.auth.RegisterTeacherRequest;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.RoleRepository;
import com.hokori.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    @Autowired(required = false)
    private FirebaseAuth firebaseAuth;

    @Autowired private JwtConfig jwtConfig;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration:3600}")
    private Long jwtExpiration; // hiện chưa dùng — giữ lại cho cấu hình

    /* ===================== FIREBASE LOGIN ===================== */
    public AuthResponse authenticateUser(String idToken) throws FirebaseAuthException {
        if (firebaseAuth == null) {
            throw new RuntimeException("Firebase authentication is not configured. Please set firebase.enabled=true");
        }
        FirebaseToken decoded = firebaseAuth.verifyIdToken(idToken);

        String firebaseUid = decoded.getUid();
        String email       = decoded.getEmail();
        String displayName = decoded.getName();
        String photoUrl    = decoded.getPicture();

        // Lấy thêm thông tin từ claims
        Boolean emailVerified = decoded.isEmailVerified();
        String provider = null;
        Object firebaseClaim = decoded.getClaims().get("firebase");
        if (firebaseClaim instanceof Map<?, ?> map) {
            Object sp = map.get("sign_in_provider"); // "password", "google.com", ...
            if (sp != null) provider = String.valueOf(sp);
        }

        User user = findOrCreateUser(firebaseUid, email, displayName, photoUrl);

        boolean updated = false;
        if (emailVerified != null && !emailVerified.equals(user.getFirebaseEmailVerified())) {
            user.setFirebaseEmailVerified(emailVerified);
            updated = true;
        }
        if (provider != null && (user.getFirebaseProvider() == null || !provider.equals(user.getFirebaseProvider()))) {
            user.setFirebaseProvider(provider);
            updated = true;
        }
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user); // lưu một lần là đủ (kể cả có updated hay không)
        return createAuthResponse(user, "firebase");
    }

    /* ===================== REFRESH TOKEN ===================== */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            if (!jwtConfig.isRefreshToken(refreshToken)) throw new RuntimeException("Invalid refresh token");
            String email = jwtConfig.extractUsername(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (Boolean.FALSE.equals(user.getIsActive())) throw new RuntimeException("User account is deactivated");
            return createAuthResponse(user, "refresh");
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    /* ===================== FIND OR CREATE USER (FIREBASE) ===================== */
    public User findOrCreateUser(String firebaseUid, String email, String displayName, String photoUrl) {
        // 1) Tìm theo firebaseUid
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

        // 2) Nếu chưa có uid → map theo email, gộp tài khoản cũ
        if (email != null) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.setFirebaseUid(firebaseUid);
                boolean updated = false;

                if (displayName != null && !displayName.equals(user.getDisplayName())) { user.setDisplayName(displayName); updated = true; }
                if (photoUrl    != null && !photoUrl.equals(user.getAvatarUrl()))      { user.setAvatarUrl(photoUrl);    updated = true; }
                if (!Boolean.TRUE.equals(user.getIsVerified())) { user.setIsVerified(true); updated = true; }
                if (user.getRole() == null) { user.setRole(getDefaultRole()); updated = true; }

                if (updated) userRepository.save(user);
                return user;
            }
        }

        // 3) Tạo user mới
        User u = new User();
        u.setFirebaseUid(firebaseUid);
        u.setEmail(email);
        u.setDisplayName(displayName);
        u.setAvatarUrl(photoUrl);
        u.setIsActive(true);
        u.setIsVerified(true);
        u.setLearningLanguage("Japanese");
        u.setCurrentJlptLevel(User.JLPTLevel.N5);
        u.setRole(getDefaultRole()); // LEARNER mặc định
        return userRepository.save(u);
    }

    /* ===================== ROLE HELPERS ===================== */
    private Role getDefaultRole() {
        return roleRepository.findByRoleName("LEARNER")
                .orElseThrow(() -> new RuntimeException("Default role LEARNER not found"));
    }

    public void assignRoleToUser(User user, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        user.setRole(role);
        userRepository.save(user);
    }

    public boolean userHasRole(User user, String roleName) {
        return user.getRole() != null && roleName.equals(user.getRole().getRoleName());
    }
    public boolean isAdmin(User user) { return userHasRole(user, "ADMIN"); }
    public boolean isStaffOrAdmin(User user) { return userHasRole(user, "STAFF") || userHasRole(user, "ADMIN"); }
    public boolean canCreateContent(User user) { return userHasRole(user, "TEACHER") || userHasRole(user, "STAFF") || userHasRole(user, "ADMIN"); }
    public List<String> getUserRoles(User user) { return (user.getRole() != null) ? List.of(user.getRole().getRoleName()) : List.of(); }

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
    public String getEmailFromToken(String token) { return jwtConfig.extractUsername(token); }

    private AuthResponse createAuthResponse(User user, String loginType) {
        List<String> roles = getUserRoles(user);

        String accessToken  = jwtTokenService.generateAccessToken(user, loginType, roles);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        // jwtTokenService.getTokenExpiration() trả về Long (epoch millis hoặc epoch seconds)
        Long exp = jwtTokenService.getTokenExpiration();
        // Nếu lớn hơn ~ 3e9 thì coi là milliseconds, ngược lại là seconds
        Instant expiresAt = (exp != null)
                ? (exp > 3_000_000_000L ? Instant.ofEpochMilli(exp) : Instant.ofEpochSecond(exp))
                : null;

        return new AuthResponse(
                accessToken,
                refreshToken,
                expiresAt,
                new AuthResponse.UserInfo(user),
                roles
        );
    }

    /* ===================== USERNAME/PASSWORD LOGIN ===================== */
    public AuthResponse authenticateWithUsernamePassword(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseGet(() -> userRepository.findByEmail(loginRequest.getUsername())
                        .orElseThrow(() -> new RuntimeException("Invalid username/email or password")));
        if (Boolean.FALSE.equals(user.getIsActive())) throw new RuntimeException("User account is deactivated");
        if (user.getPasswordHash() == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash()))
            throw new RuntimeException("Invalid username/email or password");

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return createAuthResponse(user, "password");
    }

    /* ===================== OLD COMBINED REGISTER (giữ tương thích) ===================== */
    public AuthResponse registerUser(RegisterRequest req) {
        if (!req.isPasswordConfirmed()) throw new RuntimeException("Password and confirmation do not match");
        if (!req.isValidRole()) throw new RuntimeException("Invalid role selected");
        if (userRepository.findByUsername(req.getUsername()).isPresent())
            throw new RuntimeException("Username already exists");
        if (userRepository.findByEmail(req.getEmail()).isPresent())
            throw new RuntimeException("Email already exists");

        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setDisplayName(req.getDisplayName() != null ? req.getDisplayName() : req.getUsername());
        u.setCountry(req.getCountry());
        u.setNativeLanguage(req.getNativeLanguage());
        u.setLearningLanguage("Japanese");

        String uid = "username_" + req.getUsername() + "_" + System.currentTimeMillis();
        u.setFirebaseUid(uid);

        u.setCurrentJlptLevel(parseJlptOrDefault(req.getCurrentJlptLevel(), User.JLPTLevel.N5));
        u.setIsActive(true);
        u.setIsVerified(false);

        String roleName = (req.getRoleName() != null) ? req.getRoleName() : "LEARNER";
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        u.setRole(role);

        u = userRepository.save(u);
        return createAuthResponse(u, "registration");
    }

    /* ===================== NEW: SEPARATE REGISTRATION ===================== */
    public AuthResponse registerLearner(RegisterLearnerRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent())
            throw new RuntimeException("Username already exists");
        if (userRepository.findByEmail(req.getEmail()).isPresent())
            throw new RuntimeException("Email already exists");

        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setDisplayName((req.getDisplayName()!=null && !req.getDisplayName().isBlank())
                ? req.getDisplayName() : req.getUsername());
        u.setCountry(req.getCountry());
        u.setNativeLanguage(req.getNativeLanguage());
        u.setLearningLanguage("Japanese");

        String uid = "username_" + req.getUsername() + "_" + System.currentTimeMillis();
        u.setFirebaseUid(uid);

        u.setCurrentJlptLevel(parseJlptOrDefault(req.getCurrentJlptLevel(), User.JLPTLevel.N5));
        u.setIsActive(true);
        u.setIsVerified(false);

        Role learner = roleRepository.findByRoleName("LEARNER")
                .orElseThrow(() -> new RuntimeException("Role LEARNER not found"));
        u.setRole(learner);

        u = userRepository.save(u);
        return createAuthResponse(u, "registration");
    }

    public AuthResponse registerTeacher(RegisterTeacherRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent())
            throw new RuntimeException("Username already exists");
        if (userRepository.findByEmail(req.getEmail()).isPresent())
            throw new RuntimeException("Email already exists");

        // yêu cầu tối thiểu: bio >= 50, JLPT N2/N1
        if (req.getBio() == null || req.getBio().trim().length() < 50)
            throw new RuntimeException("Bio must be at least 50 characters");
        if (!"N1".equalsIgnoreCase(req.getCurrentJlptLevel()) &&
                !"N2".equalsIgnoreCase(req.getCurrentJlptLevel()))
            throw new RuntimeException("Teacher must have JLPT N2 or N1");

        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));

        // hồ sơ (đã gộp vào User)
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setDisplayName((req.getFirstName() != null ? req.getFirstName() : "") +
                (req.getLastName()!= null ? " " + req.getLastName() : ""));
        u.setHeadline(req.getHeadline());
        u.setBio(req.getBio());
        u.setCurrentJlptLevel(parseJlptOrDefault(req.getCurrentJlptLevel(), User.JLPTLevel.N2));
        u.setLearningLanguage("Japanese");
        u.setIsActive(true);
        u.setIsVerified(false);

        // social / website
        u.setWebsiteUrl(req.getWebsiteUrl());
        u.setFacebook(req.getFacebook());
        u.setInstagram(req.getInstagram());
        u.setLinkedin(req.getLinkedin());
        u.setTiktok(req.getTiktok());
        u.setX(req.getX());
        u.setYoutube(req.getYoutube());

        // trạng thái duyệt mặc định
        u.setApprovedStatus(User.ApproveStatus.NONE);

        // UID giả cho tài khoản username/password
        String uid = "username_" + req.getUsername() + "_" + System.currentTimeMillis();
        u.setFirebaseUid(uid);

        Role teacher = roleRepository.findByRoleName("TEACHER")
                .orElseThrow(() -> new RuntimeException("Role TEACHER not found"));
        u.setRole(teacher);

        u = userRepository.save(u);
        return createAuthResponse(u, "registration");
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

    /* ===================== Helpers ===================== */
    private User.JLPTLevel parseJlptOrDefault(String s, User.JLPTLevel def) {
        if (s == null || s.isBlank()) return def;
        try { return User.JLPTLevel.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return def; }
    }
}
