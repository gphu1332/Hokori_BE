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
        Integer passScorePercent,  // Điểm % tối thiểu để pass (null = không yêu cầu)
        Boolean passed,            // true = đã pass, false = chưa pass, null = chưa có passScorePercent
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {}
