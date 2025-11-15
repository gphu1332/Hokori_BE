// com.hokori.web.dto.flashcard.FlashcardSetResponse.java
package com.hokori.web.dto.flashcard;

import com.hokori.web.Enum.FlashcardSetType;
import com.hokori.web.entity.FlashcardSet;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class FlashcardSetResponse {

    Long id;
    String title;
    String description;
    String level;
    FlashcardSetType type;
    Long createdByUserId;
    Instant createdAt;
    Instant updatedAt;

    public static FlashcardSetResponse fromEntity(FlashcardSet set) {
        return FlashcardSetResponse.builder()
                .id(set.getId())
                .title(set.getTitle())
                .description(set.getDescription())
                .level(set.getLevel())
                .type(set.getType())
                .createdByUserId(set.getCreatedBy().getId())
                .createdAt(set.getCreatedAt())
                .updatedAt(set.getUpdatedAt())
                .build();
    }
}
