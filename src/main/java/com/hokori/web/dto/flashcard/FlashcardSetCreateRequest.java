package com.hokori.web.dto.flashcard;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlashcardSetCreateRequest {

    @NotBlank
    private String title;

    private String description;

    // Ví dụ: N5, N4, Beginner...
    private String level;
}
