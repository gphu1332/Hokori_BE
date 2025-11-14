package com.hokori.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="course")
@Getter @Setter
public class Course extends BaseEntity {

    @Column(name = "cover_image_path", length = 500)
    private String coverImagePath;

    @Column(nullable=false) private String title;
    @Column(nullable=false, unique=true, length=180) private String slug;
    private String subtitle;

    @Column(columnDefinition="TEXT") private String description;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private JLPTLevel level = JLPTLevel.N5;

    private Long priceCents;
    private Long discountedPriceCents;
    private String currency = "VND";

    @Column(nullable=false) private Long userId; // teacher owner
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private CourseStatus status = CourseStatus.DRAFT;
    private Instant publishedAt;

    private Double ratingAvg = 0.0;
    private Long ratingCount = 0L;
    private Long enrollCount = 0L;

    // trong Course
    @JsonIgnore
    @OneToMany(mappedBy="course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex asc")
    private List<Chapter> chapters = new ArrayList<>();

}

