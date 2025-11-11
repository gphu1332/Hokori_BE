package com.hokori.web.entity;

import com.hokori.web.Enum.ApprovalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Entity
@Table(name = "profile_approve_request",
        indexes = {@Index(name = "ix_par_user", columnList = "user_id")})
public class ProfileApproveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owner (người nộp)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_par_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "reviewed_by")  private Long reviewedBy;
    @Column(name = "reviewed_at")  private LocalDateTime reviewedAt;

    @Lob @Column(name = "note") private String note;

    // Items
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileApproveRequestItem> items = new ArrayList<>();

    // Audit
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate(){ createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate protected void onUpdate(){ updatedAt = LocalDateTime.now(); }
}
