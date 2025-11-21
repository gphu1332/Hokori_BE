package com.hokori.web.dto.jlpt;

import com.hokori.web.Enum.JlptQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JlptQuestionUpdateRequest {

    @NotBlank
    private String content;

    @NotNull
    private JlptQuestionType questionType;

    private String explanation;

    private Integer orderIndex;

    // media đường dẫn (relative path)
    private String audioPath;      // optional
    private String imagePath;      // optional
    private String imageAltText;   // optional
}
