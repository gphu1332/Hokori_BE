package com.hokori.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "course_rejection_reason_detail")
@Getter
@Setter
public class CourseRejectionReasonDetail extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType; // CHAPTER, LESSON, or SECTION
    
    @Column(name = "item_id", nullable = false)
    private Long itemId; // ID of the chapter/lesson/section
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;
    
    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId; // Moderator who rejected
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    public enum ItemType {
        CHAPTER, LESSON, SECTION
    }
}

