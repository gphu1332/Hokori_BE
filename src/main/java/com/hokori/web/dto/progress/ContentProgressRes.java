package com.hokori.web.dto.progress;

import com.hokori.web.Enum.ContentFormat;
import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class ContentProgressRes {
    private Long contentId;
    private ContentFormat contentFormat;
    private Boolean isTrackable;
    private Long lastPositionSec;
    private Boolean isCompleted;
    private Long durationSec; // optional lấy từ Asset nếu là video
}
