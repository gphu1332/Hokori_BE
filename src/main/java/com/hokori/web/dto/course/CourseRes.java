package com.hokori.web.dto.course;

import com.hokori.web.Enum.ContentFormat;
import com.hokori.web.Enum.ContentType;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRes {
    private Long id;
    private String title;
    private String slug;
    private String subtitle;
    private String description;
    private JLPTLevel level;
    private Long priceCents;
    private Long discountedPriceCents;
    private String currency;
    private String coverImagePath;
    private CourseStatus status;
    private Instant publishedAt;
    private Long userId;
    private String teacherName; // Tên giáo viên (displayName hoặc username)
    private List<ChapterRes> chapters;
    private Long enrollCount;
}
