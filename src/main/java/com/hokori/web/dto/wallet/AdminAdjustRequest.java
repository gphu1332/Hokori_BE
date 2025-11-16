package com.hokori.web.dto.wallet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminAdjustRequest {

    @NotNull
    private Long userId;

    /**
     * amountCents có thể dương (cộng) hoặc âm (trừ).
     * Ví dụ: +100_000 = thưởng, -50_000 = phạt.
     */
    @NotNull
    @Min(1)
    private Long amountCentsAbs; // trị tuyệt đối

    @NotNull
    private Boolean increase; // true = cộng, false = trừ

    private String description;
}
