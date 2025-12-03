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
    
    // Selected option ID (null if user hasn't answered this question yet)
    Long selectedOptionId;

    public static JlptQuestionWithOptionsResponse fromEntity(
            JlptQuestion q,
            List<JlptOptionResponse> options
    ) {
        return fromEntity(q, options, null);
    }
    
    public static JlptQuestionWithOptionsResponse fromEntity(
            JlptQuestion q,
            List<JlptOptionResponse> options,
            Long selectedOptionId
    ) {
        // Map audioPath to audioUrl with /files/ prefix for serving
        String audioUrl = null;
        if (q.getAudioPath() != null && !q.getAudioPath().isEmpty()) {
            // If audioPath already has /files/ prefix, use it as is
            // Otherwise, add /files/ prefix
            if (q.getAudioPath().startsWith("/files/")) {
                audioUrl = q.getAudioPath();
            } else {
                audioUrl = "/files/" + q.getAudioPath();
            }
        } else if (q.getAudioUrl() != null && !q.getAudioUrl().isEmpty()) {
            // Fallback to audioUrl field if audioPath is null
            audioUrl = q.getAudioUrl().startsWith("/files/") 
                    ? q.getAudioUrl() 
                    : "/files/" + q.getAudioUrl();
        }
        
        return JlptQuestionWithOptionsResponse.builder()
                .id(q.getId())
                .testId(q.getTest().getId())
                .type(q.getQuestionType() != null ? q.getQuestionType().name() : null)
                .content(q.getContent())
                .audioUrl(audioUrl)           // map path -> audioUrl with /files/ prefix
                .questionType(q.getQuestionType())
                .explanation(q.getExplanation())
                .orderIndex(q.getOrderIndex())
                .imagePath(q.getImagePath())
                .imageAltText(q.getImageAltText())
                .options(options)
                .selectedOptionId(selectedOptionId)  // null if not answered yet
                .build();
    }
}
