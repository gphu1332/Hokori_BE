// com.hokori.web.dto.jlpt.JlptOptionCreateRequest.java
package com.hokori.web.dto.jlpt;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JlptOptionCreateRequest {

    @NotBlank
    private String content;

    private Boolean correct = false;

    private Integer orderIndex;

    private String imagePath;
    private String imageAltText;
}
