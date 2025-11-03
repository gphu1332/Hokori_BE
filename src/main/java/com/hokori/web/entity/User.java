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
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_username", columnList = "username", unique = true),
                @Index(name = "idx_users_firebase_uid", columnList = "firebase_uid", unique = true)
        })
@ToString(exclude = "role")
public class User {

    // ===== Core identity =====
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Liên kết Firebase (nếu đăng nhập bằng Firebase) */
    @Size(max = 128)
    @Column(name = "firebase_uid", unique = true)
    private String firebaseUid;

    /** Provider Firebase (web/ios/android) – giữ để debug/truy vết */
    @Size(max = 50)
    @Column(name = "firebase_provider")
    private String firebaseProvider;

    /** Email do Firebase xác minh? (giữ vì có cờ này trong token Firebase) */
    @Column(name = "firebase_email_verified")
    private Boolean firebaseEmailVerified;

    // ===== Account =====
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(unique = true, nullable = false)
    private String email;

    @Size(max = 100)
    @Column(unique = true)
    private String username;

    /** Hash mật khẩu cho luồng đăng nhập “tự quản” (không bắt buộc nếu dùng Firebase hoàn toàn) */
    @Size(max = 255)
    @Column(name = "password_hash")
    private String passwordHash;

    /** Hoạt động / bị khoá */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /** Đã xác minh danh tính (KYC/Email) */
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    // ===== Profile =====
    @Size(max = 255)
    @Column(name = "display_name")
    private String displayName;

    @Size(max = 100)
    @Column(name = "first_name")
    private String firstName;

    @Size(max = 100)
    @Column(name = "last_name")
    private String lastName;

    @Size(max = 500)
    @Column(name = "avatar_url")
    private String avatarUrl;

    @Size(max = 500)
    @Column(name = "banner_url")
    private String bannerUrl;

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

    @Size(max = 100)
    private String city;

    @Size(max = 255)
    private String address;

    @Size(max = 50)
    @Column(name = "native_language")
    private String nativeLanguage;

    @Size(max = 50)
    @Column(name = "learning_language")
    private String learningLanguage = "Japanese";

    @Enumerated(EnumType.STRING)
    @Column(name = "current_jlpt_level", length = 10)
    private JLPTLevel currentJlptLevel = JLPTLevel.N5;

    /** Tóm tắt hồ sơ: headline hiển thị ngắn, bio chi tiết hơn (theo DB design) */
    @Size(max = 150)
    @Column(name = "headline")
    private String headline;

    @Lob
    @Column(name = "bio")
    private String bio;

    /** Số năm kinh nghiệm (dành cho teacher) */
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    /** Sở thích/phong cách dạy (giữ dạng TEXT để flexible) */
    @Lob
    @Column(name = "teaching_styles")
    private String teachingStyles;

    // ===== Social / website =====
    @Size(max = 255) @Column(name = "website_url")  private String websiteUrl;
    @Size(max = 255) @Column(name = "facebook")     private String facebook;
    @Size(max = 255) @Column(name = "instagram")    private String instagram;
    @Size(max = 255) @Column(name = "linkedin")     private String linkedin;
    @Size(max = 255) @Column(name = "tiktok")       private String tiktok;
    @Size(max = 255) @Column(name = "x_twitter")    private String x;         // twitter/X
    @Size(max = 255) @Column(name = "youtube")      private String youtube;

    // ===== Approval (phục vụ luồng duyệt hồ sơ giáo viên) =====
    @Enumerated(EnumType.STRING)
    @Column(name = "approved_status", length = 20)
    private ApproveStatus approvedStatus = ApproveStatus.NONE; // NONE/PENDING/APPROVED/REJECTED

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    // ===== Payout / Bank (teacher) =====
    @Size(max = 100) @Column(name = "bank_account_number") private String bankAccountNumber;
    @Size(max = 150) @Column(name = "bank_account_name")   private String bankAccountName;
    @Size(max = 150) @Column(name = "bank_name")           private String bankName;
    @Size(max = 150) @Column(name = "bank_branch_name")    private String bankBranchName;

    /** Ngày thanh toán gần nhất (đồng bộ với lịch 15/30 hàng tháng) */
    @Column(name = "last_payout_date")
    private LocalDate lastPayoutDate;

    // ===== Audit =====
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Xoá mềm để giữ toàn vẹn dữ liệu */
    @Column(name = "deleted_flag")
    private Boolean deletedFlag = false;

    // ===== Relationship =====
    /** Hiện tại vẫn ManyToOne Role để giữ tương thích; nếu DB dùng bảng trung gian user_roles, sẽ migrate sau */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "role_id", referencedColumnName = "id")
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

    // ===== Enums =====
    public enum Gender { MALE, FEMALE, OTHER }
    public enum JLPTLevel { N5, N4, N3, N2, N1 }
    public enum ApproveStatus { NONE, PENDING, APPROVED, REJECTED }

    // ===== Audit hooks =====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
