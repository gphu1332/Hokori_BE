package com.hokori.web.dto.flashcard;

public record ReviewCardResponse(
        long reviewCount,
        boolean mastered
) {}
