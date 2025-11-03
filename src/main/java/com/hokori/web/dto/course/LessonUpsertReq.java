package com.hokori.web.dto.course;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LessonUpsertReq {
    @NotBlank private String title;
    private Integer orderIndex;
    private Long totalDurationSec;
}
