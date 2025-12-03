// com.hokori.web.dto.jlpt.JlptTestResultResponse.java
package com.hokori.web.dto.jlpt;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JlptTestResultResponse {

    Long testId;
    Long userId;
    int totalQuestions;
    int correctCount;
    double score;          // Tổng điểm = totalScore * correctCount / totalQuestions
    String level;          // N5 / N4 / ...
    Double passScore;      // Điểm cần để đậu theo level (đã scale theo total_score của đề)
    Boolean passed;        // true = đậu, false = rớt
    
    // ==== Điểm từng phần ====
    SectionScore grammarVocab;  // Grammar + Vocab (gộp chung)
    SectionScore reading;      // Reading
    SectionScore listening;    // Listening
    
    @Value
    @Builder
    public static class SectionScore {
        int totalQuestions;    // Tổng số câu trong phần này
        int correctCount;      // Số câu đúng
        double score;          // Điểm của phần này (tính theo tỷ lệ)
        double maxScore;       // Điểm tối đa của phần này
    }
}
