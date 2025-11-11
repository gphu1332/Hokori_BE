package com.hokori.web.dto.quiz;

public record QuizUpsertReq(
        String title,
        String description,
        Integer timeLimitSec,      // null = unlimited
        Integer passScorePercent   // 0..100
) {}
