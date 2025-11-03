package com.hokori.web.dto.course;

import com.hokori.web.Enum.ContentFormat;
import com.hokori.web.Enum.ContentType;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import lombok.AllArgsConstructor; import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data @AllArgsConstructor
public class CourseRes {
    private Long id; private String title; private String slug; private String subtitle;
    private String description; private JLPTLevel level;
    private Long priceCents; private Long discountedPriceCents; private String currency; private Long coverAssetId;
    private CourseStatus status; private Instant publishedAt; private Long userId;
    private List<ChapterRes> chapters;
}
//@Data @AllArgsConstructor class ChapterRes {
//    private Long id; private String title; private Integer orderIndex; private String summary;
//    private List<LessonRes> lessons;
//}
//@Data @AllArgsConstructor class LessonRes {
//    private Long id; private String title; private Integer orderIndex; private Long totalDurationSec;
//    private List<SectionRes> sections;
//}
//@Data @AllArgsConstructor class SectionRes {
//    private Long id; private String title; private Integer orderIndex; private ContentType studyType;
//    private Long flashcardSetId; // VOCAB mapping
//    private List<ContentRes> contents;
//}
//@Data @AllArgsConstructor class ContentRes {
//    private Long id; private Integer orderIndex; private ContentFormat contentFormat;
//    private boolean primaryContent;
//    private Long assetId; private String richText; private Long quizId; private Long flashcardSetId;
//}
