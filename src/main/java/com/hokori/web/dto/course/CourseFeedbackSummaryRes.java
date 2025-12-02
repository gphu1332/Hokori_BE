package com.hokori.web.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CourseFeedbackSummaryRes {
    private double ratingAvg;
    private long ratingCount;
}
