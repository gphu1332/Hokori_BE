package com.hokori.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hokori.web.entity.TeacherProfile;
import com.hokori.web.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// entity/TeacherApprovalRequest.java
@Data
@Entity
@Table(name = "teacher_approval_requests")
public class TeacherApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    @JsonIgnore
    private TeacherProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Status status = Status.PENDING; // PENDING|APPROVED|REJECTED

    @Column(columnDefinition = "TEXT")
    private String messageFromTeacher; // lời nhắn khi gửi yêu cầu

    @Column(columnDefinition = "TEXT")
    private String reviewNote; // ghi chú của admin khi duyệt

    private LocalDateTime submittedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    @JsonIgnore
    private User reviewer; // admin duyệt

    public enum Status { PENDING, APPROVED, REJECTED }
}

