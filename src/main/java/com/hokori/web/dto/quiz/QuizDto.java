package com.hokori.web.dto.quiz;

public record QuizDto(
        Long id,
        Long lessonId,
        String title,
        String description,
        Integer totalQuestions,
        Integer timeLimitSec,
        Integer passScorePercent
) {}
