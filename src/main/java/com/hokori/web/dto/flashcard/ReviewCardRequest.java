package com.hokori.web.dto.flashcard;

public record ReviewCardRequest(
        Boolean mastered // optional: true nếu user đánh dấu đã master
) {}
