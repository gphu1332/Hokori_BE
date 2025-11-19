package com.hokori.web.dto.course;

import com.hokori.web.Enum.ContentFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContentRes {
    private Long id;
    private Integer orderIndex;
    private ContentFormat contentFormat;
    private boolean primaryContent;
    private String filePath;
    private String richText;
    private Long flashcardSetId;
}
