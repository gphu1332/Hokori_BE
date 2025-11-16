package com.hokori.web.service;

import com.hokori.web.constants.RoleConstants;
import com.hokori.web.entity.Role;
import com.hokori.web.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {
    
    @Autowired
    private RoleRepository roleRepository;
    
    /**
     * Initialize default roles if they don't exist.
     * Uses RoleConstants to avoid hardcoding role names and descriptions.
     * Ensures role names are normalized to uppercase for consistency (PostgreSQL is case-sensitive).
     */
    public void initializeDefaultRoles() {
        for (String roleName : RoleConstants.DEFAULT_ROLE_NAMES) {
            // Normalize role name to uppercase for consistency
            String normalizedRoleName = roleName.trim().toUpperCase();
            
            // Check if role exists (case-insensitive check via repository)
            if (!roleRepository.findByRoleName(normalizedRoleName).isPresent()) {
                Role role = new Role();
                role.setRoleName(normalizedRoleName); // Always store uppercase
                role.setDescription(RoleConstants.getDescription(normalizedRoleName));
                roleRepository.save(role);
            }
        }
    }
    
    /**
     * Get all roles
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    
    /**
     * Get role by name (case-insensitive).
     * Normalizes role name to uppercase before searching for consistency.
     */
    public Optional<Role> getRoleByName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return Optional.empty();
        }
        // Normalize to uppercase for consistency (repository uses case-insensitive query)
        String normalizedRoleName = roleName.trim().toUpperCase();
        return roleRepository.findByRoleName(normalizedRoleName);
    }
    
    /**
     * Get role by ID
     */
    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }
    
    /**
     * Create a new role.
     * Normalizes role name to uppercase for consistency (PostgreSQL is case-sensitive).
     * 
     * @param roleName Role name (will be normalized to uppercase)
     * @param description Role description
     * @return Created role
     * @throws IllegalArgumentException if role already exists
     */
    public Role createRole(String roleName, String description) {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        
        // Normalize role name to uppercase for consistency
        String normalizedRoleName = roleName.trim().toUpperCase();
        
        // Check if role already exists (case-insensitive)
        if (roleRepository.findByRoleName(normalizedRoleName).isPresent()) {
            throw new IllegalArgumentException("Role '" + normalizedRoleName + "' already exists");
        }
        
        Role role = new Role();
        role.setRoleName(normalizedRoleName); // Always store uppercase
        role.setDescription(description);
        return roleRepository.save(role);
    }
    
    /**
     * Update role description
     */
    public Role updateRole(Long id, String description) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        role.setDescription(description);
        return roleRepository.save(role);
    }
    
    /**
     * Delete role (only if no users assigned)
     */
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        
        // Check if any users have this role
        if (role.getUsers() != null && !role.getUsers().isEmpty()) {
            throw new RuntimeException("Cannot delete role with assigned users");
        }
        
        roleRepository.delete(role);
    }
    
    /**
     * Check if role exists (case-insensitive).
     * Normalizes role name to uppercase before checking for consistency.
     */
    public boolean roleExists(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return false;
        }
        // Normalize to uppercase for consistency (repository uses case-insensitive query)
        String normalizedRoleName = roleName.trim().toUpperCase();
        return roleRepository.findByRoleName(normalizedRoleName).isPresent();
    }
    
    /**
     * Get default role for new users.
     * Always returns LEARNER role (uppercase, normalized).
     */
    public Role getDefaultRole() {
        return roleRepository.findByRoleName(RoleConstants.LEARNER)
                .orElseThrow(() -> new RuntimeException("Default role " + RoleConstants.LEARNER + " not found. Please run initializeDefaultRoles() first."));
    }
}
