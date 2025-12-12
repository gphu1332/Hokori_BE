package com.hokori.web.dto.course;

import com.hokori.web.Enum.ContentFormat;
import lombok.Data;

@Data
public class ContentUpsertReq {

    private Integer orderIndex;

    // ASSET / RICH_TEXT / FLASHCARD_SET
    private ContentFormat contentFormat;

    private boolean primaryContent;

    // ASSET
    private String filePath;

    // RICH_TEXT
    private String richText;

    private Long flashcardSetId;

    // QUIZ
    private Long quizId;
}
