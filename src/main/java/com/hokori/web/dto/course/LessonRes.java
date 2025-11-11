package com.hokori.web.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data @AllArgsConstructor
public class LessonRes {
    private Long id;
    private String title;
    private Integer orderIndex;
    private Long totalDurationSec;
    private List<SectionRes> sections;
}
