package com.hokori.web.dto.jlpt;

import com.hokori.web.Enum.JLPTLevel;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JlptTestStartResponse {

    private Long testId;
    private Long userId;
    private JLPTLevel level;
    private Integer durationMin;
    private Integer totalScore;
    private Instant startedAt;

    // SỐ NGƯỜI ĐANG THI – FE cần
    private Integer currentParticipants;

    // gửi luôn đề cho FE render
    private List<JlptQuestionWithOptionsResponse> questions;
}
