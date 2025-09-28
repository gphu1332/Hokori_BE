package com.hokori.web.service;

import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.RoleRepository;
import com.hokori.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }
    
    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }
    
    public List<User> getUsersByRole(String roleName) {
        return userRepository.findByRoleName(roleName);
    }
    
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    public void deactivateUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(false);
            userRepository.save(user);
        }
    }
    
    public void activateUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(true);
            userRepository.save(user);
        }
    }
    
    public void assignRole(Long userId, String roleName) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
        
        if (userOpt.isPresent() && roleOpt.isPresent()) {
            User user = userOpt.get();
            Role role = roleOpt.get();
            
            user.setRoleId(role.getId());
            userRepository.save(user);
        }
    }
    
    public void removeRole(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRoleId(null);
            userRepository.save(user);
        }
    }
    
    public List<String> getUserRoles(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && userOpt.get().getRole() != null) {
            return List.of(userOpt.get().getRole().getRoleName());
        }
        return List.of();
    }
    
    public boolean userHasRole(Long userId, String roleName) {
        Optional<User> userOpt = userRepository.findById(userId);
        return userOpt.isPresent() && 
               userOpt.get().getRole() != null && 
               userOpt.get().getRole().getRoleName().equals(roleName);
    }
    
    public void updateLastLogin(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    public long getUserCount() {
        return userRepository.count();
    }
    
    public long getUserCountByRole(String roleName) {
        Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
        if (roleOpt.isPresent()) {
            return userRepository.countByRoleId(roleOpt.get().getId());
        }
        return 0;
    }
    
    public List<User> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);
    }
    
    public List<User> getUsersWithRecentLogin(LocalDateTime date) {
        return userRepository.findUsersWithRecentLogin(date);
    }
    
    public void initializeDefaultRoles() {
        // Create default roles if they don't exist
        String[] defaultRoles = {"LEARNER", "TEACHER", "STAFF", "ADMIN"};
        String[] descriptions = {
            "Regular student/learner",
            "Teacher who can create content", 
            "Staff member with limited admin access",
            "Full system administrator"
        };
        
        for (int i = 0; i < defaultRoles.length; i++) {
            if (!roleRepository.existsByRoleName(defaultRoles[i])) {
                Role role = new Role(defaultRoles[i], descriptions[i]);
                roleRepository.save(role);
            }
        }
    }
}
