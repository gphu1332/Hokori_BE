package com.hokori.web.dto.jlpt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JlptTestUpdateRequest {

    @NotBlank
    private String level;          // N5, N4, ...

    @Min(1)
    private Integer durationMin;   // thời lượng làm bài

    @Min(1)
    private Integer totalScore;    // tổng điểm tối đa

    // optional
    private String resultNote;
}
