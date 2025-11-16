// com.hokori.web.dto.flashcard.FlashcardResponse.java
package com.hokori.web.dto.flashcard;

import com.hokori.web.entity.Flashcard;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class FlashcardResponse {

    Long id;
    Long setId;
    String frontText;
    String backText;
    String reading;
    String exampleSentence;
    Integer orderIndex;
    Instant createdAt;
    Instant updatedAt;

    public static FlashcardResponse fromEntity(Flashcard card) {
        return FlashcardResponse.builder()
                .id(card.getId())
                .setId(card.getSet().getId())
                .frontText(card.getFrontText())
                .backText(card.getBackText())
                .reading(card.getReading())
                .exampleSentence(card.getExampleSentence())
                .orderIndex(card.getOrderIndex())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
