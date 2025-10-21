package com.hokori.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "roles")
@Data
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "role_name", unique = true, nullable = false)
    private String roleName;
    
    @Size(max = 255)
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationship
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<User> users;
    
    // Constructors
    public Role() {}
    
    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return roleName != null ? roleName.equals(role.roleName) : role.roleName == null;
    }
    
    @Override
    public int hashCode() {
        return roleName != null ? roleName.hashCode() : 0;
    }
}