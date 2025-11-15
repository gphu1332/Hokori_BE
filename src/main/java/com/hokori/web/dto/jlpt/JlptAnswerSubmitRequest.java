// com.hokori.web.dto.jlpt.JlptAnswerSubmitRequest.java
package com.hokori.web.dto.jlpt;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JlptAnswerSubmitRequest {

    @NotNull
    private Long questionId;

    @NotNull
    private Long selectedOptionId;
}
