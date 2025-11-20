package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_certificates",
        indexes = {@Index(name = "ix_user_cert_user", columnList = "user_id")})
public class UserCertificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owner
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_cert_user"))
    private User user;

    @Column(length = 255, nullable = false) private String title;
    @Column(name = "issue_date")  private LocalDate issueDate;
    @Column(name = "expiry_date") private LocalDate expiryDate;

    @Column(name = "credential_id", length = 255)  private String credentialId;
    @Column(name = "credential_url", length = 1000) private String credentialUrl;

    // File (nếu có)
    @Column(name = "file_url", length = 1000)  private String fileUrl;
    @Column(name = "file_name", length = 255)  private String fileName;
    @Column(name = "mime_type", length = 100)  private String mimeType;
    @Column(name = "file_size_bytes")          private Long fileSizeBytes;
    @Column(name = "storage_provider", length = 50) private String storageProvider;

    // Verify (tuỳ dùng)
    @Column(name = "verified_by") private Long verifiedBy;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;

    @Column(name = "note") private String note;

    // Audit
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate(){ createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate protected void onUpdate(){ updatedAt = LocalDateTime.now(); }
}
