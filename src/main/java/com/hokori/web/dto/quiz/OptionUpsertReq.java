package com.hokori.web.dto.quiz;

public record OptionUpsertReq(
        String content,
        Boolean isCorrect,
        Integer orderIndex
) {}
