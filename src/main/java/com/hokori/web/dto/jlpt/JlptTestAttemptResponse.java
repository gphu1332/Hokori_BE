// com.hokori.web.dto.jlpt.JlptTestAttemptResponse.java
package com.hokori.web.dto.jlpt;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Response DTO cho 1 lần làm bài JLPT Test (attempt).
 * Dùng để hiển thị lịch sử các lần làm bài.
 */
@Value
@Builder
public class JlptTestAttemptResponse {

    Long id;
    Long testId;
    Long userId;
    Instant startedAt;
    Instant submittedAt;
    
    // Kết quả tổng
    int totalQuestions;
    int correctCount;
    double score;
    boolean passed;
    
    // Điểm từng phần
    SectionScore grammarVocab;
    SectionScore reading;
    SectionScore listening;
    
    @Value
    @Builder
    public static class SectionScore {
        Integer totalQuestions;
        Integer correctCount;
        Double score;
    }
}

