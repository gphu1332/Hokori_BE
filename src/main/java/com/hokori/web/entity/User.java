package com.hokori.web.entity;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.Enum.Gender;
import com.hokori.web.Enum.JLPTLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "users",
        indexes = {
                // Giữ các index này để Hibernate tạo index; với SQL Server, UNIQUE trên nullable vẫn chỉ cho 1 NULL.
                // Khuyến nghị: với SQL Server, tạo UNIQUE FILTERED INDEX qua migration: WHERE username IS NOT NULL / firebase_uid IS NOT NULL.
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_username", columnList = "username" /*, unique = true*/), // [CHANGED] bỏ unique ở đây để tránh "1 NULL duy nhất" trên SQL Server
                @Index(name = "idx_users_firebase_uid", columnList = "firebase_uid" /*, unique = true*/ ), // [CHANGED]
                @Index(name = "idx_users_role_id", columnList = "role_id"), // [CHANGED] thêm index thực dụng
                @Index(name = "idx_users_approval_status", columnList = "approval_status") // [CHANGED] thêm index thực dụng
        })
@ToString(exclude = {"role", "currentApproveRequest"})
public class User {

    // ===== Core identity =====
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // OK với SQL Server (IDENTITY)
    private Long id;

    @Size(max = 128)
    @Column(name = "firebase_uid" /*, unique = true*/) // [CHANGED] bỏ unique ở level @Column (xem giải thích trên)
    private String firebaseUid;

    @Size(max = 50)
    @Column(name = "firebase_provider")
    private String firebaseProvider;

    @Column(name = "firebase_email_verified")
    private Boolean firebaseEmailVerified;

    // ===== Account =====
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Size(max = 100)
    @Column(name = "username", length = 100 /*, unique = true*/) // [CHANGED] bỏ unique ở level @Column (xem giải thích trên)
    private String username;

    /** Hash mật khẩu cho luồng đăng nhập “tự quản” */
    @Size(max = 255)
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** Hoạt động / bị khoá */
    @Column(
            name = "is_active",
            nullable = false
            // Let Hibernate handle database-specific syntax (works with both SQL Server and PostgreSQL)
            // Default value set in Java code: isActive = true
    )
    private Boolean isActive = true;

    /** Đã xác minh danh tính (KYC/Email) */
    @Column(
            name = "is_verified",
            nullable = false
            // Let Hibernate handle database-specific syntax (works with both SQL Server and PostgreSQL)
            // Default value set in Java code: isVerified = false
    )
    private Boolean isVerified = false;

    // ===== Profile =====
    @Size(max = 255)
    @Column(name = "display_name", length = 255)
    private String displayName;

    @Size(max = 100)
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Size(max = 100)
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Size(max = 500)
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Size(max = 500)
    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Size(max = 20)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender = Gender.OTHER;

    @Size(max = 255) @Column(name = "address", length = 255)   private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_jlpt_level", length = 10)
    private JLPTLevel currentJlptLevel = JLPTLevel.N5;

//    @Lob
    @Column(name = "bio")
    @JsonIgnore // Prevent serialization to avoid LOB stream errors (use DTO/mapper instead)
    private String bio;

    /** Số năm kinh nghiệm (dành cho teacher) */
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // ===== Social / website =====
    @Size(max = 255) @Column(name = "website_url", length = 255) private String websiteUrl;
    @Size(max = 255) @Column(name = "linkedin", length = 255)     private String linkedin;

    // ===== Approval (luồng duyệt teacher) =====
    @Enumerated(EnumType.STRING)
    @Column(
            name = "approval_status",
            length = 20,
            nullable = false
            // Let Hibernate handle database-specific syntax (works with both SQL Server and PostgreSQL)
            // Default value set in Java code: approvalStatus = ApprovalStatus.NONE
    )
    private ApprovalStatus approvalStatus = ApprovalStatus.NONE; // NONE/PENDING/APPROVED/REJECTED

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    /** Tham chiếu request gần nhất (nếu muốn hiển thị nhanh tiến độ) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_approval_request_id",
            foreignKey = @ForeignKey(name = "fk_users_profile_approval_request"))
    @JsonIgnore // Prevent LazyInitializationException when serializing to JSON
    private ProfileApproveRequest currentApproveRequest;

    // ===== Payout / Bank (teacher) =====
    @Size(max = 100) @Column(name = "bank_account_number", length = 100) private String bankAccountNumber;
    @Size(max = 150) @Column(name = "bank_account_name", length = 150)   private String bankAccountName;
    @Size(max = 150) @Column(name = "bank_name", length = 150)           private String bankName;
    @Size(max = 150) @Column(name = "bank_branch_name", length = 150)    private String bankBranchName;

    @Column(name = "last_payout_date")
    private LocalDate lastPayoutDate;

    @Column(
            name = "wallet_balance",
            nullable = false
            // Let Hibernate handle database-specific syntax (works with both SQL Server and PostgreSQL)
            // Default value set in Java code: walletBalance = 0L
    )
    private Long walletBalance = 0L;


    // ===== Audit =====
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Xoá mềm để giữ toàn vẹn dữ liệu */
    @Column(
            name = "deleted_flag",
            nullable = false
            // Let Hibernate handle database-specific syntax (works with both SQL Server and PostgreSQL)
            // Default value set in Java code: deletedFlag = false
    )
    private Boolean deletedFlag = false;

    // ===== Relationship =====
    /** Tạm thời ManyToOne Role; nếu chuyển sang user_roles (N-N) sẽ migrate sau */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "role_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_users_role"))
    private Role role;

    // ===== Constructors =====
    public User() {}
    public User(String firebaseUid, String email, String username, String displayName) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.username = username;
        this.displayName = displayName;
    }

    // ===== Helpers =====
    public boolean hasRole(String roleName) {
        return role != null && role.getRoleName().equals(roleName);
    }

    @Transient
    public Long getRoleId() {
        return role != null ? role.getId() : null;
    }

    // ===== Audit hooks =====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
