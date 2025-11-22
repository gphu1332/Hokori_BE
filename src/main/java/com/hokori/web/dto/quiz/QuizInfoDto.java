package com.hokori.web.dto.quiz;

/**
 * DTO for quiz info (metadata) - used before starting a quiz attempt
 */
public record QuizInfoDto(
        Long quizId,
        String title,
        String description,
        Integer totalQuestions,
        Integer timeLimitSec,
        Integer passScorePercent,
        Long attemptCount  // Number of attempts this user has made
) {}

