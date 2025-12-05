// com.hokori.web.dto.jlpt.JlptTestResultResponse.java
package com.hokori.web.dto.jlpt;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

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
    
    // Thông tin attempt (chỉ có khi đã submit bài)
    Long attemptId;        // ID của attempt (null nếu chưa nộp bài)
    Instant startedAt;     // Thời gian bắt đầu làm bài (null nếu chưa nộp bài)
    Instant submittedAt;    // Thời gian nộp bài (null nếu chưa nộp bài)
    
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
