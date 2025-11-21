package com.hokori.web.dto.flashcard;


public record FlashcardSetUpdateRequest(
        String title,
        String description,
        String level
) {}
