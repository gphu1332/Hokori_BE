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
import java.util.HashMap;
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
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration;
    
    public AuthResponse authenticateUser(String idToken) throws FirebaseAuthException {
        if (firebaseAuth == null) {
            throw new RuntimeException("Firebase authentication is not configured. Please set firebase.enabled=true in application.properties");
        }
        
        // Verify Firebase token
        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
        
        // Extract user information from Firebase token
        String firebaseUid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String displayName = decodedToken.getName();
        String photoUrl = decodedToken.getPicture();
        
        // Find or create user
        User user = findOrCreateUser(firebaseUid, email, displayName, photoUrl);
        
        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Get user roles
        List<String> roles = getUserRoles(user);
        
        // Generate JWT tokens
        String accessToken = jwtConfig.generateToken(user.getEmail(), new HashMap<>());
        String refreshToken = jwtConfig.generateRefreshToken(user.getEmail());
        
        // Create response
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user);
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtExpiration,
                userInfo,
                roles
        );
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Validate refresh token
            if (!jwtConfig.isRefreshToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }
            
            String email = jwtConfig.extractUsername(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!user.getIsActive()) {
                throw new RuntimeException("User account is deactivated");
            }
            
            // Generate new tokens
            String newAccessToken = jwtConfig.generateToken(user.getEmail(), new HashMap<>());
            String newRefreshToken = jwtConfig.generateRefreshToken(user.getEmail());
            
            // Get user roles
            List<String> roles = getUserRoles(user);
            
            // Create response
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user);
            
            return new AuthResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtExpiration,
                    userInfo,
                    roles
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token", e);
        }
    }
    
    public User findOrCreateUser(String firebaseUid, String email, String displayName, String photoUrl) {
        Optional<User> existingUser = userRepository.findByFirebaseUid(firebaseUid);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Update user information if needed
            boolean updated = false;
            if (displayName != null && !displayName.equals(user.getDisplayName())) {
                user.setDisplayName(displayName);
                updated = true;
            }
            if (photoUrl != null && !photoUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(photoUrl);
                updated = true;
            }
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                updated = true;
            }
            
            if (updated) {
                userRepository.save(user);
            }
            
            return user;
        } else {
            // Create new user
            User newUser = new User();
            newUser.setFirebaseUid(firebaseUid);
            newUser.setEmail(email);
            newUser.setDisplayName(displayName);
            newUser.setAvatarUrl(photoUrl);
            newUser.setIsActive(true);
            newUser.setIsVerified(true); // Firebase verified users are considered verified
            
            // Save user
            newUser = userRepository.save(newUser);
            
            // Assign default role (LEARNER)
            assignDefaultRole(newUser);
            
            return newUser;
        }
    }
    
    private void assignDefaultRole(User user) {
        Role learnerRole = roleRepository.findByRoleName("LEARNER")
                .orElseThrow(() -> new RuntimeException("Default role LEARNER not found"));
        
        user.setRoleId(learnerRole.getId());
        userRepository.save(user);
    }
    
    public List<String> getUserRoles(User user) {
        if (user.getRole() != null) {
            return List.of(user.getRole().getRoleName());
        }
        return List.of();
    }
    
    public boolean hasRole(User user, String roleName) {
        return user.getRole() != null && user.getRole().getRoleName().equals(roleName);
    }
    
    public void assignRole(User user, String roleName) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        user.setRoleId(role.getId());
        userRepository.save(user);
    }
    
    public void removeRole(User user) {
        user.setRoleId(null);
        userRepository.save(user);
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public boolean validateToken(String token) {
        try {
            String email = jwtConfig.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            return jwtConfig.validateToken(token, user.getEmail()) && user.getIsActive();
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getEmailFromToken(String token) {
        return jwtConfig.extractUsername(token);
    }
    
    // Username/Password Authentication
    public AuthResponse authenticateWithUsernamePassword(LoginRequest loginRequest) {
        // Find user by username or email
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseGet(() -> userRepository.findByEmail(loginRequest.getUsername())
                        .orElseThrow(() -> new RuntimeException("Invalid username/email or password")));
        
        // Check if account is active
        if (!user.getIsActive()) {
            throw new RuntimeException("User account is deactivated");
        }
        
        // Verify password
        if (user.getPasswordHash() == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid username/email or password");
        }
        
        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Get user roles
        List<String> roles = getUserRoles(user);
        
        // Generate JWT tokens
        String accessToken = jwtConfig.generateToken(user.getEmail(), new HashMap<>());
        String refreshToken = jwtConfig.generateRefreshToken(user.getEmail());
        
        // Create response
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user);
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtExpiration,
                userInfo,
                roles
        );
    }
    
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setDisplayName(registerRequest.getDisplayName() != null ? registerRequest.getDisplayName() : registerRequest.getUsername());
        newUser.setCountry(registerRequest.getCountry());
        newUser.setNativeLanguage(registerRequest.getNativeLanguage());
        newUser.setCurrentJlptLevel(registerRequest.getCurrentJlptLevel());
        newUser.setIsActive(true);
        newUser.setIsVerified(false); // Require email verification for regular signup
        newUser.setLearningLanguage("Japanese");
        
        // Save user
        newUser = userRepository.save(newUser);
        
        // Assign default role (LEARNER)
        assignDefaultRole(newUser);
        
        // Get user roles
        List<String> roles = getUserRoles(newUser);
        
        // Generate JWT tokens
        String accessToken = jwtConfig.generateToken(newUser.getEmail(), new HashMap<>());
        String refreshToken = jwtConfig.generateRefreshToken(newUser.getEmail());
        
        // Create response
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(newUser);
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtExpiration,
                userInfo,
                roles
        );
    }
}
