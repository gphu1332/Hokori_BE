// com.hokori.web.dto.flashcard.FlashcardProgressUpdateRequest.java
package com.hokori.web.dto.flashcard;

import com.hokori.web.Enum.FlashcardProgressStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FlashcardProgressUpdateRequest {

    /**
     * Trạng thái sau khi user học xong thẻ:
     * - NEW, LEARNING, MASTERED
     */
    @NotNull
    private FlashcardProgressStatus status;
}
