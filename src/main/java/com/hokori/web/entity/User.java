package com.hokori.web.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
@ToString(exclude = "role")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 128)
    @Column(name = "firebase_uid", unique = true)
    private String firebaseUid;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(unique = true, nullable = false)
    private String email;

    @Size(max = 100)
    @Column(unique = true)
    private String username;

    @Size(max = 255)
    @Column(name = "password_hash")
    private String passwordHash;

    @Size(max = 255)
    @Column(name = "display_name")
    private String displayName;

    @Size(max = 500)
    @Column(name = "avatar_url")
    private String avatarUrl;

    @Size(max = 20)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender = Gender.OTHER;

    @Size(max = 100)
    private String country;

    @Size(max = 50)
    @Column(name = "native_language")
    private String nativeLanguage;

    @Size(max = 50)
    @Column(name = "learning_language")
    private String learningLanguage = "Japanese";

    @Enumerated(EnumType.STRING)
    @Column(name = "current_jlpt_level", length = 10)
    private JLPTLevel currentJlptLevel = JLPTLevel.N5;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "role_id", referencedColumnName = "id") // roles.id là PK
    private Role role;

    public User() {}

    public User(String firebaseUid, String email, String username, String displayName) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.username = username;
        this.displayName = displayName;
    }

    public boolean hasRole(String roleName) {
        return role != null && role.getRoleName().equals(roleName);
    }

    public enum Gender { MALE, FEMALE, OTHER }
    public enum JLPTLevel { N5, N4, N3, N2, N1 }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Đọc nhanh roleId mà không map trùng cột
    @Transient
    public Long getRoleId() {
        return role != null ? role.getId() : null;
    }
}
