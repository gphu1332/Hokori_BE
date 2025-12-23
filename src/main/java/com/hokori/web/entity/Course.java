package com.hokori.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="course")
@DynamicInsert // Only insert non-null fields (excludes snapshot_data when null)
@DynamicUpdate // Only update changed fields
@Getter @Setter
public class Course extends BaseEntity {

    @Column(name = "cover_image_path", length = 500)
    private String coverImagePath;

    @Column(nullable=false) private String title;
    @Column(nullable=false, unique=true, length=180) private String slug;
    private String subtitle;

    @Column(columnDefinition="TEXT") 
    @JsonIgnore // Prevent serialization to avoid LOB stream errors
    private String description;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private JLPTLevel level = JLPTLevel.N5;

    private Long priceCents;
    private Long discountedPriceCents;
    private String currency = "VND";

    @Column(nullable=false) private Long userId; // teacher owner
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private CourseStatus status = CourseStatus.DRAFT;
    private Instant publishedAt;
    
    // Pending update info (khi teacher submit update từ PUBLISHED course)
    @Column(name = "pending_update_at")
    private Instant pendingUpdateAt; // Timestamp khi teacher submit update
    
    // Snapshot data (JSON) để lưu nội dung cũ khi submit update
    // Use updatable=false, insertable=false to prevent Hibernate from auto-updating this JSONB column
    // We use native query (updateSnapshotData) to update this field with proper JSONB cast
    @Column(name = "snapshot_data", columnDefinition = "jsonb", updatable = false, insertable = false)
    @JsonIgnore // Prevent serialization to avoid LOB stream errors
    private String snapshotData; // JSON snapshot của course tree (chapters, lessons, sections, contents)
    
    // Rejection info (khi moderator reject)
    @Column(name = "rejection_reason", columnDefinition="TEXT")
    private String rejectionReason;
    private Instant rejectedAt;
    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;
    
    // Flag info (khi moderator flag course)
    @Column(name = "flagged_reason", columnDefinition="TEXT")
    private String flaggedReason; // Lý do flag (tổng hợp từ các flags)
    private Instant flaggedAt;
    @Column(name = "flagged_by_user_id")
    private Long flaggedByUserId; // Moderator đã flag

    private Double ratingAvg = 0.0;
    private Long ratingCount = 0L;
    private Long enrollCount = 0L;
    
    // Comment control (moderator can disable comments for problematic courses)
    @Column(name = "comments_disabled", nullable = false)
    private Boolean commentsDisabled = false; // Default: comments enabled

    // trong Course
    @JsonIgnore
    @OneToMany(mappedBy="course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex asc")
    private List<Chapter> chapters = new ArrayList<>();

}

