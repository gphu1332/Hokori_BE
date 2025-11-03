package com.hokori.web.dto.course;


import com.hokori.web.Enum.ContentFormat;
import lombok.Data;

@Data
public class ContentUpsertReq {
    private Integer orderIndex;
    private ContentFormat contentFormat; // ASSET/RICH_TEXT/FLASHCARD_SET/QUIZ_REF
    private boolean primaryContent;
    private Long assetId;
    private String richText;
    private Long quizId;
    private Long flashcardSetId; // chỉ khi dùng phương án B
}
