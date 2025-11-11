package com.hokori.web.dto.course;

import com.hokori.web.Enum.ContentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data @AllArgsConstructor
public class SectionRes {
    private Long id;
    private String title;
    private Integer orderIndex;
    private ContentType studyType;
    private Long flashcardSetId;
    private List<ContentRes> contents;
}
