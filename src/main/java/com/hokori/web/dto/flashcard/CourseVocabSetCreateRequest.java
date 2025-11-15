// com.hokori.web.dto.flashcard.CourseVocabSetCreateRequest.java
package com.hokori.web.dto.flashcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseVocabSetCreateRequest {

    @NotBlank
    private String title;

    private String description;

    private String level;

    /**
     * ID của SectionsContent có type "vocabulary"
     * mà teacher muốn gắn bộ flashcard này vào.
     */
    @NotNull
    private Long sectionContentId;
}
