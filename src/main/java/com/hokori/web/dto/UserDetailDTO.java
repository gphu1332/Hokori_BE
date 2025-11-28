package com.hokori.web.dto;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDetailDTO {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String roleName;
    private Boolean isActive;
    private Boolean isVerified;
    private JLPTLevel currentJlptLevel;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime updatedAt;

    public static UserDetailDTO from(User u) {
        UserDetailDTO d = new UserDetailDTO();
        d.setId(u.getId());
        d.setUsername(u.getUsername());
        d.setEmail(u.getEmail());
        d.setDisplayName(u.getDisplayName());
        d.setRoleName(u.getRole() != null ? u.getRole().getRoleName() : null);
        d.setIsActive(u.getIsActive());
        d.setIsVerified(u.getIsVerified());
        d.setCurrentJlptLevel(u.getCurrentJlptLevel());
        d.setCreatedAt(u.getCreatedAt());
        d.setLastLoginAt(u.getLastLoginAt());
        d.setUpdatedAt(u.getUpdatedAt());
        return d;
    }
}

