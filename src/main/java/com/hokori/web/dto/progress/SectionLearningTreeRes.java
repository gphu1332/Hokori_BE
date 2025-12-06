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
    private List<ContentLearningTreeRes> contents;
}

