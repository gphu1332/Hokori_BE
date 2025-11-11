package com.hokori.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleAssignmentRequest {
    
    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String roleName;
    
    // Constructors
    public RoleAssignmentRequest() {}
    
    public RoleAssignmentRequest(String roleName) {
        this.roleName = roleName;
    }
    
    // Getters and Setters
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    @Override
    public String toString() {
        return "RoleAssignmentRequest{" +
                "roleName='" + roleName + '\'' +
                '}';
    }
}
