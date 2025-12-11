package com.hokori.web.dto.progress;

import com.hokori.web.Enum.ContentType;
import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SectionLearningTreeRes {
    private Long sectionId;
    private String title;
    private Integer orderIndex;
    private ContentType studyType;
    private Long flashcardSetId;
    private Long quizId; // Quiz ID if exists (quiz now belongs to section, not lesson)
    private Integer quizPassScorePercent; // Pass score % for quiz (null if no quiz or no pass requirement)
    private Boolean quizPassed; // Has user passed the quiz? (null if no quiz or no attempts)
    private Integer quizBestScore; // Best score achieved (null if no attempts)
    private List<ContentLearningTreeRes> contents;
}

