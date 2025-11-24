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

    // FE cần "type"
    String type;             // LISTENING / READING / VOCAB / GRAMMAR

    String content;

    // FE cần audioUrl (map từ audioPath trong entity)
    String audioUrl;         // null nếu không có

    JlptQuestionType questionType; // nếu bạn vẫn muốn giữ thêm info
    String explanation;
    Integer orderIndex;
    String imagePath;
    String imageAltText;
    List<JlptOptionResponse> options;

    public static JlptQuestionWithOptionsResponse fromEntity(
            JlptQuestion q,
            List<JlptOptionResponse> options
    ) {
        return JlptQuestionWithOptionsResponse.builder()
                .id(q.getId())
                .testId(q.getTest().getId())
                .type(q.getQuestionType() != null ? q.getQuestionType().name() : null)
                .content(q.getContent())
                .audioUrl(q.getAudioPath())           // map path -> audioUrl
                .questionType(q.getQuestionType())
                .explanation(q.getExplanation())
                .orderIndex(q.getOrderIndex())
                .imagePath(q.getImagePath())
                .imageAltText(q.getImageAltText())
                .options(options)
                .build();
    }
}
