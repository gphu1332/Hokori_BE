package com.hokori.web.dto.quiz;

import java.util.List;

public record PlayQuestionDto(
        Long questionId,
        String content,
        String questionType,
        Integer orderIndex,
        List<PlayOptionDto> options
) {}
