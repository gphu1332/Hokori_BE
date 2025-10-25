package com.hokori.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore // tránh vòng lặp khi serialize profile riêng lẻ
    private User user;

    @Size(max = 100) private String lastName;
    @Size(max = 100) private String firstName;
    @Size(max = 60)  private String headline;
    @Size(min = 50, max = 5000) @Column(columnDefinition = "TEXT")
    private String bio;

    @Size(max = 255) private String websiteUrl;
    @Size(max = 255) private String facebook;
    @Size(max = 255) private String instagram;
    @Size(max = 255) private String linkedin;
    @Size(max = 255) private String tiktok;
    @Size(max = 255) private String x;
    @Size(max = 255) private String youtube;
    @Size(max = 50)  private String language = "Tiếng Việt";

    /* ====== BẰNG CẤP / CHỨNG CHỈ / KINH NGHIỆM ====== */
    @Size(max = 100) private String highestDegree;     // Cử nhân/Thạc sĩ/…
    @Size(max = 255) private String major;             // Chuyên ngành
    @Min(0) @Max(60) private Integer yearsOfExperience;

    @Column(columnDefinition = "TEXT")
    private String certifications; // JSON hoặc chuỗi “;” phân tách tên chứng chỉ (JLPT N1, JFT, …)

    @Column(columnDefinition = "TEXT")
    private String evidenceUrls;   // link drive/website tới bằng cấp (phân tách bằng ‘;’)

    /* ====== TRẠNG THÁI DUYỆT ====== */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT; // DRAFT|PENDING|APPROVED|REJECTED

    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "rejected_at") private LocalDateTime rejectedAt;

    public enum ApprovalStatus { DRAFT, PENDING, APPROVED, REJECTED }
}
