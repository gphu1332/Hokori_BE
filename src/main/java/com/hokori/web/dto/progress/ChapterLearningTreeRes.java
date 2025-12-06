package com.hokori.web.dto.progress;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChapterLearningTreeRes {
    private Long chapterId;
    private String title;
    private Integer orderIndex;
    private String summary;
    private Integer progressPercent; // Progress % of this chapter
    private List<LessonLearningTreeRes> lessons;
}

