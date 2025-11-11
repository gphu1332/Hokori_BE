package com.hokori.web.dto.quiz;

public record OptionDto(
        Long id,
        String content,
        Boolean isCorrect,
        Integer orderIndex
) {}
