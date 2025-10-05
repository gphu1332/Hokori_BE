package com.hokori.web.config;

import com.hokori.web.entity.Role;
import com.hokori.web.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired(required = false)
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Skip role initialization since database already has role data
        System.out.println("DataInitializer: Using existing database roles");
    }

    private void initializeDefaultRoles() {
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
                role.setCreatedAt(LocalDateTime.now());
                role.setUpdatedAt(LocalDateTime.now());
                roleRepository.save(role);
                System.out.println("Created role: " + defaultRoles[i]);
            }
        }
    }
}
