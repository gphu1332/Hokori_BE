package com.hokori.web.dto.flashcard;

public record FlashcardDashboardResponse(
        long totalSets,
        long totalCards,
        long reviewedToday,
        int streakDays
) {}
