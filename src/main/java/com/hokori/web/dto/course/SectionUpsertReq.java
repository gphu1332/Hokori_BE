package com.hokori.web.dto.course;

import com.hokori.web.Enum.ContentType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SectionUpsertReq {
    @NotBlank private String title;
    private Integer orderIndex;
    private ContentType studyType;   // GRAMMAR/VOCABULARY/KANJI/QUIZ
    private Long flashcardSetId;   // bắt buộc nếu VOCABULARY
}
