package com.hokori.web.service;

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
     * Initialize default roles if they don't exist
     */
    public void initializeDefaultRoles() {
        String[] roleNames = {"LEARNER", "TEACHER", "STAFF", "ADMIN"};
        String[] descriptions = {
            "Regular student/learner",
            "Teacher who can create content", 
            "Staff member with limited admin access",
            "Full system administrator"
        };
        
        for (int i = 0; i < roleNames.length; i++) {
            if (!roleRepository.findByRoleName(roleNames[i]).isPresent()) {
                Role role = new Role();
                role.setRoleName(roleNames[i]);
                role.setDescription(descriptions[i]);
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
     * Get role by name
     */
    public Optional<Role> getRoleByName(String roleName) {
        return roleRepository.findByRoleName(roleName);
    }
    
    /**
     * Get role by ID
     */
    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }
    
    /**
     * Create a new role
     */
    public Role createRole(String roleName, String description) {
        Role role = new Role();
        role.setRoleName(roleName);
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
     * Check if role exists
     */
    public boolean roleExists(String roleName) {
        return roleRepository.findByRoleName(roleName).isPresent();
    }
    
    /**
     * Get default role for new users
     */
    public Role getDefaultRole() {
        return roleRepository.findByRoleName("LEARNER")
                .orElseThrow(() -> new RuntimeException("Default role LEARNER not found"));
    }
}
