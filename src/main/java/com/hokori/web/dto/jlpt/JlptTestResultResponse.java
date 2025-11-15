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
    double score;          // nếu mỗi câu = totalScore / totalQuestions
}
