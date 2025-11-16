// com.hokori.web.dto.flashcard.UserFlashcardProgressResponse.java
package com.hokori.web.dto.flashcard;

import com.hokori.web.Enum.FlashcardProgressStatus;
import com.hokori.web.entity.UserFlashcardProgress;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class UserFlashcardProgressResponse {

    Long id;
    Long userId;
    Long flashcardId;
    FlashcardProgressStatus status;
    Instant masteredAt;
    Instant lastReviewedAt;
    int reviewCount;

    public static UserFlashcardProgressResponse fromEntity(UserFlashcardProgress p) {
        return UserFlashcardProgressResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .flashcardId(p.getFlashcard().getId())
                .status(p.getStatus())
                .masteredAt(p.getMasteredAt())
                .lastReviewedAt(p.getLastReviewedAt())
                .reviewCount(p.getReviewCount())
                .build();
    }
}
