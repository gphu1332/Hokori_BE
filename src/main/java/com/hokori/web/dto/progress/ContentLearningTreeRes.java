package com.hokori.web.dto.progress;

import com.hokori.web.Enum.ContentFormat;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContentLearningTreeRes {
    private Long contentId;
    private Integer orderIndex;
    private ContentFormat contentFormat;
    private Boolean isPrimaryContent;
    private String filePath;
    private String richText;
    private Long flashcardSetId;
    
    // Progress info
    private Boolean isTrackable;
    private Long lastPositionSec; // For video resume
    private Boolean isCompleted;
    private Long durationSec; // Optional: video duration
}

