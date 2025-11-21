package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "profile_approve_request_item",
        indexes = {@Index(name = "ix_pari_request", columnList = "request_id")})
public class ProfileApproveRequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Request
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pari_request"))
    private ProfileApproveRequest request;

    // (optional) tham chiếu certificate gốc trên hồ sơ user (snapshot dưới, không buộc)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_certificate_id",
            foreignKey = @ForeignKey(name = "fk_pari_source_cert"))
    private UserCertificate sourceCertificate;

    // --- Snapshot thông tin lúc nộp (tránh sửa cert gốc làm sai request cũ)
    @Column(length = 255, nullable = false) private String title;
    @Column(name = "issue_date")  private LocalDate issueDate;
    @Column(name = "expiry_date") private LocalDate expiryDate;
    @Column(name = "credential_id", length = 255)  private String credentialId;
    @Column(name = "credential_url", length = 1000) private String credentialUrl;

    @Column(name = "file_url", length = 1000)  private String fileUrl;
    @Column(name = "file_name", length = 255)  private String fileName;
    @Column(name = "mime_type", length = 100)  private String mimeType;
    @Column(name = "file_size_bytes")          private Long fileSizeBytes;
    @Column(name = "storage_provider", length = 50) private String storageProvider;

    // Verify từng item (tuỳ)
    @Column(name = "verified_by") private Long verifiedBy;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;

    @Column(name = "note") private String note;

    // Audit
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate(){ createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate protected void onUpdate(){ updatedAt = LocalDateTime.now(); }
}
