package com.hokori.web.dto.comment;

import java.time.Instant;
import java.util.List;

public record CourseCommentDto(
        Long id,
        Long parentId,
        Long courseId,
        Long userId,
        String authorName,
        String avatarUrl,
        String content,
        boolean edited,
        Instant createdAt,
        Instant updatedAt,
        List<CourseCommentDto> replies
) {}
