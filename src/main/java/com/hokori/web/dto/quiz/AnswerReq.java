package com.hokori.web.dto.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** Payload trả lời 1 câu hỏi trong attempt */
public record AnswerReq(
        // FE có thể gửi "optionId" hoặc "option_id" đều map vào đây
        @JsonProperty("optionId")   @NotNull Long optionId,
        @JsonProperty("option_id")          Long _alias // cho phép snake_case; không dùng trực tiếp
) {
    public Long optionId() {
        // ưu tiên optionId, fallback option_id
        return optionId != null ? optionId : _alias;
    }
}
