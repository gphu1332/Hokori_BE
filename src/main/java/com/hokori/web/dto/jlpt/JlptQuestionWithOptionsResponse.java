// com.hokori.web.dto.jlpt.JlptQuestionWithOptionsResponse.java
package com.hokori.web.dto.jlpt;

import com.hokori.web.Enum.JlptQuestionType;
import com.hokori.web.entity.JlptQuestion;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class JlptQuestionWithOptionsResponse {

    Long id;
    Long testId;
    String content;
    JlptQuestionType questionType;
    String explanation;
    Integer orderIndex;
    String audioPath;
    String imagePath;
    String imageAltText;
    List<JlptOptionResponse> options;

    public static JlptQuestionWithOptionsResponse fromEntity(JlptQuestion q,
                                                             List<JlptOptionResponse> options) {
        return JlptQuestionWithOptionsResponse.builder()
                .id(q.getId())
                .testId(q.getTest().getId())
                .content(q.getContent())
                .questionType(q.getQuestionType())
                .explanation(q.getExplanation())
                .orderIndex(q.getOrderIndex())
                .audioPath(q.getAudioPath())
                .imagePath(q.getImagePath())
                .imageAltText(q.getImageAltText())
                .options(options)
                .build();
    }
}
