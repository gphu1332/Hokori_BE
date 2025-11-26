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
    // ==== mới thêm ====
    String level;      // N5 / N4 / ...
    Double passScore;  // điểm cần để đậu theo level (đã scale theo total_score của đề)
    Boolean passed;    // true = đậu, false = rớt
}
