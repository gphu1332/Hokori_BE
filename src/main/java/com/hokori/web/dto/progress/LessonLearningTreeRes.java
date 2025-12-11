package com.hokori.web.dto.progress;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LessonLearningTreeRes {
    private Long lessonId;
    private String title;
    private Integer orderIndex;
    private Long totalDurationSec;
    private Boolean isCompleted; // Is lesson completed?
    private Long quizId; // Quiz ID if exists
    private Integer quizPassScorePercent; // Pass score % for quiz (null if no quiz or no pass requirement)
    private Boolean quizPassed; // Has user passed the quiz? (null if no quiz or no attempts)
    private Integer quizBestScore; // Best score achieved (null if no attempts)
    private List<SectionLearningTreeRes> sections;
}

