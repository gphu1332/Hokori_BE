package com.hokori.web.service;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;

    public UserService(UserRepository userRepository, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
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
        // Nếu bạn có currentApproveRequest thì giữ nguyên; chưa có thì bỏ qua
        userRepository.save(u);

        res.put("approvalStatus", u.getApprovalStatus());
        res.put("approvedAt", u.getApprovedAt());
        res.put("timestamp", LocalDateTime.now());
        // res.put("currentApproveRequestId", u.getCurrentApproveRequest()!=null ? u.getCurrentApproveRequest().getId() : null);
        return res;
    }
}
