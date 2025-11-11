package com.hokori.web.dto.progress;

import lombok.*;
import java.time.Instant;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class EnrollmentLiteRes {
    private Long enrollmentId;
    private Long courseId;
    private Integer progressPercent;
    private Instant startedAt;
    private Instant completedAt;
    private Instant lastAccessAt;
}
