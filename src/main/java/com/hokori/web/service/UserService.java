package com.hokori.web.service;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.dto.AdminCreateUserRequest;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleService roleService,
                       FileStorageService fileStorageService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    /* ===== READ ===== */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllWithRole();
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) { return userRepository.findById(id); }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) { return userRepository.findByEmail(email); }

    @Transactional(readOnly = true)
    public Optional<User> getUserByFirebaseUid(String firebaseUid) { return userRepository.findByFirebaseUid(firebaseUid); }

    @Transactional(readOnly = true)
    public List<User> getActiveUsers() { return userRepository.findByIsActiveTrue(); }

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(String roleName) { return userRepository.findByRoleName(roleName); }

    @Transactional(readOnly = true)
    public List<String> getUserRoles(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole() != null ? List.of(u.getRole().getRoleName()) : Collections.<String>emptyList())
                .orElse(Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public boolean userHasRole(Long userId, String roleName) {
        // Case-insensitive check for PostgreSQL compatibility
        return userRepository.findById(userId)
                .map(u -> u.getRole() != null && u.getRole().getRoleName() != null 
                    && roleName.equalsIgnoreCase(u.getRole().getRoleName()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public long getUserCount() { return userRepository.count(); }

    @Transactional(readOnly = true)
    public long getUserCountByRole(String roleName) {
        return roleService.getRoleByName(roleName)
                .map(r -> userRepository.countByRoleId(r.getId()))
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public List<User> searchUsers(String q) { return userRepository.searchUsers(q); }

    @Transactional(readOnly = true)
    public List<User> getUsersWithRecentLogin(LocalDateTime since) {
        return userRepository.findUsersWithRecentLogin(since);
    }

    /* ===== WRITE ===== */

    public User createUser(User user) {
        if (user.getRole() == null) {
            Role defaultRole = roleService.getDefaultRole();
            user.setRole(defaultRole);
        }
        return userRepository.save(user);
    }
    
    /**
     * Admin tạo user mới với username/password và gán role
     * 
     * Lưu ý: Không có ràng buộc về bio, kể cả khi tạo TEACHER role.
     * Admin có thể tạo user với bất kỳ role nào mà không cần bio.
     */
    public User createUserByAdmin(AdminCreateUserRequest req) {
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Username already exists: " + req.getUsername());
        }
        
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Email already exists: " + req.getEmail());
        }
        
        // Kiểm tra role có tồn tại không (không có ràng buộc bio, kể cả TEACHER)
        Role role = roleService.getRoleByName(req.getRoleName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Role not found: " + req.getRoleName()));
        
        // Tạo user mới
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        
        // Set display name
        if (req.getDisplayName() != null && !req.getDisplayName().trim().isEmpty()) {
            user.setDisplayName(req.getDisplayName().trim());
        } else {
            user.setDisplayName(req.getUsername());
        }
        
        // Set firstName và lastName nếu có
        if (req.getFirstName() != null) {
            user.setFirstName(req.getFirstName());
        }
        if (req.getLastName() != null) {
            user.setLastName(req.getLastName());
        }
        
        // Set Firebase UID (tạo fake UID cho username/password account)
        String firebaseUid = "username_" + req.getUsername() + "_" + System.currentTimeMillis();
        user.setFirebaseUid(firebaseUid);
        
        // Set JLPT level
        if (req.getCurrentJlptLevel() != null && !req.getCurrentJlptLevel().trim().isEmpty()) {
            try {
                user.setCurrentJlptLevel(JLPTLevel.valueOf(req.getCurrentJlptLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Invalid JLPT level, use default
                user.setCurrentJlptLevel(JLPTLevel.N5);
            }
        } else {
            user.setCurrentJlptLevel(JLPTLevel.N5); // Default
        }
        
        // Set status
        user.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        user.setIsVerified(req.getIsVerified() != null ? req.getIsVerified() : false);
        
        // Gán role
        user.setRole(role);
        
        // Save và return
        return userRepository.save(user);
    }

    public User updateUser(User user) { return userRepository.save(user); }

    public void deleteUser(Long id) { userRepository.deleteById(id); }

    public void deactivateUser(Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setIsActive(false);
            userRepository.save(u);
        });
    }

    public void activateUser(Long id) {
        userRepository.findById(id).ifPresent(u -> {
            u.setIsActive(true);
            userRepository.save(u);
        });
    }

    public void assignRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Role role = roleService.getRoleByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        user.setRole(role);
        userRepository.save(user);
    }

    public void removeRole(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setRole(null);
            userRepository.save(u);
        });
    }

    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setLastLoginAt(LocalDateTime.now());
            userRepository.save(u);
        });
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithRole(Long id) {
        return userRepository.findByIdWithRole(id);
    }

    public Map<String, Object> submitTeacherApproval(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Map<String, Object> res = new HashMap<>();

        // Nếu đã APPROVED thì không cho submit lại
        if (u.getApprovalStatus() == ApprovalStatus.APPROVED) {
            res.put("approvalStatus", u.getApprovalStatus());
            res.put("message", "Already approved");
            res.put("approvedAt", u.getApprovedAt());
            return res;
        }

        // Chuyển sang PENDING (kể cả từ NONE/REJECTED)
        u.setApprovalStatus(ApprovalStatus.PENDING);
        u.setApprovedAt(null);
        u.setApprovedByUserId(null);
        userRepository.save(u);

        res.put("approvalStatus", u.getApprovalStatus());
        res.put("approvedAt", u.getApprovedAt());
        res.put("timestamp", LocalDateTime.now());
        return res;
    }

    /* ===== AVATAR UPLOAD ===== */

    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (Boolean.TRUE.equals(user.getDeletedFlag())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is deleted");
        }

        // Xóa avatar cũ nếu có (soft delete)
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            // Extract filePath from avatarUrl (remove /files/ prefix if exists)
            String oldFilePath = user.getAvatarUrl().startsWith("/files/")
                ? user.getAvatarUrl().substring("/files/".length())
                : user.getAvatarUrl();
            fileStorageService.deleteFile(oldFilePath);
        }

        // lưu vào thư mục uploads/avatars/{userId}
        String subFolder = "avatars/" + userId;
        String relativePath = fileStorageService.store(file, subFolder);

        String url = "/files/" + relativePath;
        user.setAvatarUrl(url);

        // @Transactional nên không cần save() lại, entity managed rồi
        return url;
    }
}
