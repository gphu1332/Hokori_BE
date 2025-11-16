// com.hokori.web.dto.jlpt.JlptTestResponse.java
package com.hokori.web.dto.jlpt;

import com.hokori.web.entity.JlptTest;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JlptTestResponse {

    Long id;
    Long eventId;
    Long createdByUserId;
    String level;
    Integer durationMin;
    Integer totalScore;
    String resultNote;

    public static JlptTestResponse fromEntity(JlptTest t) {
        return JlptTestResponse.builder()
                .id(t.getId())
                .eventId(t.getEvent().getId())
                .createdByUserId(t.getCreatedBy().getId())
                .level(t.getLevel())
                .durationMin(t.getDurationMin())
                .totalScore(t.getTotalScore())
                .resultNote(t.getResult())
                .build();
    }
}
