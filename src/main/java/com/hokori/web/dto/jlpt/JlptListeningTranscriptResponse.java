package com.hokori.web.dto.jlpt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class JlptListeningTranscriptResponse {
    private Long questionId;
    private String audioUrl;
    private String transcript;
}

