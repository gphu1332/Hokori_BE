package com.hokori.web.dto.flashcard;

public record FlashcardUpdateRequest(
        String frontText,
        String backText,
        String reading,
        String exampleSentence,
        Integer orderIndex
) {}
