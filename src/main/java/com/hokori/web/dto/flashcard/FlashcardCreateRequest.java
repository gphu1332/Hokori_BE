package com.hokori.web.dto.flashcard;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlashcardCreateRequest {

    @NotBlank
    private String frontText;          // Mặt trước (từ vựng / câu hỏi)

    @NotBlank
    private String backText;           // Mặt sau (nghĩa / đáp án)

    private String reading;            // Cách đọc (furigana, romaji...)

    private String exampleSentence;    // Ví dụ

    private Integer orderIndex;        // Thứ tự trong set
}
