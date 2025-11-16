package com.hokori.web.dto.jlpt;

import com.hokori.web.Enum.JlptEventStatus;
import com.hokori.web.entity.JlptEvent;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class JlptEventResponse {

    Long id;
    String title;
    String level;
    String description;
    JlptEventStatus status;
    LocalDateTime startAt;
    LocalDateTime endAt;
    Long createdByUserId;

    public static JlptEventResponse fromEntity(JlptEvent e) {
        return JlptEventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .level(e.getLevel())
                .description(e.getDescription())
                .status(e.getStatus())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .createdByUserId(e.getCreatedBy().getId())
                .build();
    }
}
