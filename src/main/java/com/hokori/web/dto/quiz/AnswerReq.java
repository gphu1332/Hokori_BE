package com.hokori.web.dto.quiz;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** Payload trả lời 1 câu hỏi trong attempt */
public record AnswerReq(
        @JsonProperty("optionId")
        @JsonAlias("option_id")
        @NotNull Long optionId
) {}
