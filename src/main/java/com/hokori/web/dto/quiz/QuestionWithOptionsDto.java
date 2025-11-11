package com.hokori.web.dto.quiz;

import java.util.List;

public record QuestionWithOptionsDto(
        Long id,
        String content,
        String explanation,
        String questionType,
        Integer orderIndex,
        List<OptionDto> options
) {}
