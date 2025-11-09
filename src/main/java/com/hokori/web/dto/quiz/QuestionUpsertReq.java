package com.hokori.web.dto.quiz;

public record QuestionUpsertReq(
        String content,
        String explanation,
        String questionType,  // "SINGLE_CHOICE"
        Integer orderIndex
) {}
