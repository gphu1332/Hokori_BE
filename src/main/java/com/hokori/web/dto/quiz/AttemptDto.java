package com.hokori.web.dto.quiz;

import java.time.LocalDateTime;

public record AttemptDto(
        Long id,
        Long quizId,
        String quizTitle,
        String status,
        Integer totalQuestions,
        Integer correctCount,
        Integer scorePercent,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {}
