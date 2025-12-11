package com.hokori.web.dto.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record QuizUpsertReq(
        String title,
        String description,
        Integer timeLimitSec,      // null = unlimited
        
        @Min(value = 0, message = "Pass score must be between 0 and 100")
        @Max(value = 100, message = "Pass score must be between 0 and 100")
        Integer passScorePercent   // 0..100: Điểm % tối thiểu để pass quiz (null = không yêu cầu)
) {}
