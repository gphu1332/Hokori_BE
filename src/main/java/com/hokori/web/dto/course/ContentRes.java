package com.hokori.web.dto.course;

import com.hokori.web.Enum.ContentFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ContentRes {
    private Long id;
    private Integer orderIndex;
    private ContentFormat contentFormat;
    private boolean primaryContent;
    private String filePath;
    private String richText;
    private Long flashcardSetId;
    private Long quizId;
    
    // Progress info (optional - only populated when user is enrolled)
    private Long lastPositionSec; // For video resume
    private Boolean isCompleted; // Whether content is completed
    
    // Constructor for backward compatibility (without progress)
    public ContentRes(Long id, Integer orderIndex, ContentFormat contentFormat,
                     boolean primaryContent, String filePath, String richText, Long flashcardSetId) {
        this(id, orderIndex, contentFormat, primaryContent, filePath, richText, flashcardSetId, null);
    }
    
    // Constructor with quizId (without progress)
    public ContentRes(Long id, Integer orderIndex, ContentFormat contentFormat,
                     boolean primaryContent, String filePath, String richText, Long flashcardSetId, Long quizId) {
        this.id = id;
        this.orderIndex = orderIndex;
        this.contentFormat = contentFormat;
        this.primaryContent = primaryContent;
        this.filePath = filePath;
        this.richText = richText;
        this.flashcardSetId = flashcardSetId;
        this.quizId = quizId;
        this.lastPositionSec = null;
        this.isCompleted = null;
    }
    
    // Constructor with progress
    public ContentRes(Long id, Integer orderIndex, ContentFormat contentFormat,
                     boolean primaryContent, String filePath, String richText, Long flashcardSetId,
                     Long lastPositionSec, Boolean isCompleted) {
        this(id, orderIndex, contentFormat, primaryContent, filePath, richText, flashcardSetId, null, lastPositionSec, isCompleted);
    }
    
    // Constructor with quizId and progress
    public ContentRes(Long id, Integer orderIndex, ContentFormat contentFormat,
                     boolean primaryContent, String filePath, String richText, Long flashcardSetId, Long quizId,
                     Long lastPositionSec, Boolean isCompleted) {
        this.id = id;
        this.orderIndex = orderIndex;
        this.contentFormat = contentFormat;
        this.primaryContent = primaryContent;
        this.filePath = filePath;
        this.richText = richText;
        this.flashcardSetId = flashcardSetId;
        this.quizId = quizId;
        this.lastPositionSec = lastPositionSec;
        this.isCompleted = isCompleted;
    }
}
