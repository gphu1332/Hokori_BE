package com.hokori.web.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonRes {
    private Long id;
    private String title;
    private Integer orderIndex;
    private Long totalDurationSec;
    private List<SectionRes> sections;

    // NEW: quiz thuộc lesson (1 lesson tối đa 1 quiz)
    private Long quizId;

    // Giữ constructor cũ cho các chỗ đang dùng 5 tham số
    public LessonRes(Long id, String title, Integer orderIndex,
                     Long totalDurationSec, List<SectionRes> sections) {
        this.id = id;
        this.title = title;
        this.orderIndex = orderIndex;
        this.totalDurationSec = totalDurationSec;
        this.sections = sections;
    }
}
