package com.hokori.web.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class LearningStreakRes {
    private long currentStreakDays;   // chuỗi hiện tại
    private long longestStreakDays;   // chuỗi dài nhất (optional nhưng hay)
    private LocalDate lastLearningDate;
}
