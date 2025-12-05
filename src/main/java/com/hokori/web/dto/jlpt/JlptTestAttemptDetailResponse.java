// com.hokori.web.dto.jlpt.JlptTestAttemptDetailResponse.java
package com.hokori.web.dto.jlpt;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO cho chi tiết một lần làm bài JLPT Test.
 * Bao gồm thông tin tổng quan và chi tiết từng câu hỏi với đáp án đã chọn và đáp án đúng.
 */
@Value
@Builder
public class JlptTestAttemptDetailResponse {

    // Thông tin tổng quan của attempt
    AttemptSummary summary;

    // Chi tiết từng câu hỏi
    List<QuestionDetail> questions;

    @Value
    @Builder
    public static class AttemptSummary {
        Long attemptId;
        Long testId;
        Long userId;
        Instant startedAt;
        Instant submittedAt;
        int totalQuestions;
        int correctCount;
        double score;
        String level;
        Double passScore;
        Boolean passed;
        
        // Điểm từng phần
        SectionScore grammarVocab;
        SectionScore reading;
        SectionScore listening;
    }

    @Value
    @Builder
    public static class QuestionDetail {
        Long questionId;
        String questionContent;
        String questionType;  // LISTENING / READING / VOCAB / GRAMMAR
        String explanation;
        Integer orderIndex;
        String audioUrl;       // null nếu không có
        String imagePath;      // null nếu không có
        String imageAltText;   // null nếu không có
        
        // Đáp án đã chọn (của user) - null nếu không chọn
        Long selectedOptionId;           // null nếu không chọn đáp án
        String selectedOptionContent;    // null nếu không chọn đáp án
        String selectedOptionImagePath;  // null nếu không có hoặc không chọn đáp án
        
        // Đáp án đúng
        Long correctOptionId;
        String correctOptionContent;
        String correctOptionImagePath;   // null nếu không có
        
        // Đúng hay sai
        Boolean isCorrect;
        
        // Tất cả options (để FE hiển thị)
        List<OptionDetail> allOptions;
    }

    @Value
    @Builder
    public static class OptionDetail {
        Long optionId;
        String content;
        String imagePath;      // null nếu không có
        String imageAltText;   // null nếu không có
        Integer orderIndex;
        Boolean isCorrect;     // true nếu là đáp án đúng
    }

    @Value
    @Builder
    public static class SectionScore {
        int totalQuestions;
        int correctCount;
        double score;
        double maxScore;
    }
}

