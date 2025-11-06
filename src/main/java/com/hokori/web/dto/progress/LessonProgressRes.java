package com.hokori.web.dto.progress;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class LessonProgressRes {
    private Long lessonId;
    private String title;
    private Integer orderIndex;
    private Boolean isCompleted;
}
