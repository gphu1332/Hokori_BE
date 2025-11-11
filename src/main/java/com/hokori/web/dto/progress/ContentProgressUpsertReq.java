package com.hokori.web.dto.progress;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class ContentProgressUpsertReq {
    private Long lastPositionSec;   // optional
    private Boolean isCompleted;    // optional
}
