package com.hokori.web.service;

import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service to get current authenticated user from JWT token
 */
@Service
public class CurrentUserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get current authenticated user from SecurityContext
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email);
    }
    
    /**
     * Get current user or throw exception if not found
     */
    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }
    
    /**
     * Check if current user has specific role
     */
    public boolean hasRole(String roleName) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }
        
        User user = currentUser.get();
        return user.getRole() != null && user.getRole().getRoleName().equals(roleName);
    }
    
    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    /**
     * Check if current user is teacher
     */
    public boolean isTeacher() {
        return hasRole("TEACHER");
    }
    
    /**
     * Check if current user is learner
     */
    public boolean isLearner() {
        return hasRole("LEARNER");
    }
    
    /**
     * Get current user ID
     */
    public Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }
    
    /**
     * Get current user ID or throw exception
     */
    public Long getCurrentUserIdOrThrow() {
        return getCurrentUserOrThrow().getId();
    }
}
