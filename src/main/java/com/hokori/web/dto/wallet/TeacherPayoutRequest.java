package com.hokori.web.dto.wallet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeacherPayoutRequest {

    @NotNull
    private Long teacherId;

    @NotNull
    @Min(1)
    private Long amountCents;  // số tiền muốn trả
}
