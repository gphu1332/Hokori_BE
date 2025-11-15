// com.hokori.web.dto.jlpt.JlptOptionResponse.java
package com.hokori.web.dto.jlpt;

import com.hokori.web.entity.JlptOption;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JlptOptionResponse {

    Long id;
    Long questionId;
    String content;
    Boolean correct;       // chỉ trả về cho moderator, learner tốt hơn là ẩn field này
    Integer orderIndex;
    String imagePath;
    String imageAltText;

    public static JlptOptionResponse fromEntity(JlptOption o) {
        return JlptOptionResponse.builder()
                .id(o.getId())
                .questionId(o.getQuestion().getId())
                .content(o.getContent())
                .correct(o.getIsCorrect())
                .orderIndex(o.getOrderIndex())
                .imagePath(o.getImagePath())
                .imageAltText(o.getImageAltText())
                .build();
    }
}
