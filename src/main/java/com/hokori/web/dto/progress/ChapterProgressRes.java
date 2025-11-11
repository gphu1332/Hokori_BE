package com.hokori.web.dto.progress;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class ChapterProgressRes {
    private Long chapterId;
    private String title;
    private Integer orderIndex;
    private Integer percent;
    private Stats stats;

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class Stats {
        private Integer videos;
        private Integer exercises;
        private Integer tests;
        private Long totalDurationSec;
    }
}
