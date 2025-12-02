package com.hokori.web.dto.course;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CourseFeedbackRes {

    private Long id;

    private Long userId;
    private String learnerName;
    private String learnerAvatarUrl;

    private Integer rating;
    private String comment;

    private Instant createdAt;
}
