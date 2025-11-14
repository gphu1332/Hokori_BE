package com.hokori.web.dto.course;

import com.hokori.web.entity.*;
import org.hibernate.Hibernate;

import java.util.List;
import java.util.stream.Collectors;

public final class CourseMapper {
    private CourseMapper() {}

    /* =======================
       Public API – High level
       ======================= */

    /** Map full tree (chỉ dùng khi đang ở trong 1 @Transactional để LAZY không nổ). */
    public static CourseRes toRes(Course c) {
        return new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(), c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(), c.getCoverImagePath(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                mapChaptersDeepSafe(c) // an toàn với LAZY
        );
    }

    /** Trả course chỉ với đúng 1 chapter (ví dụ: chapter học thử). */
    public static CourseRes toResOnlyTrial(Course c, Chapter trial) {
        return new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(), c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(), c.getCoverImagePath(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                List.of(toChapterResDeep(trial))
        );
    }

    /* =======================
       Chapter / Lesson / ...
       ======================= */

    /** Dùng khi tạo chapter để trả DTO mỏng, không chạm vào lesson LAZY. */
    public static ChapterRes toChapterRes(Chapter ch) {
        return new ChapterRes(
                ch.getId(),
                ch.getTitle(),
                ch.getOrderIndex(),
                ch.getSummary(),
                List.of()  // tránh đụng LAZY
        );
    }

    public static ChapterRes toChapterResDeep(Chapter ch) {
        List<LessonRes> lessons = Hibernate.isInitialized(ch.getLessons())
                ? ch.getLessons().stream().map(CourseMapper::toLessonResDeep).collect(Collectors.toList())
                : List.of();
        return new ChapterRes(
                ch.getId(), ch.getTitle(), ch.getOrderIndex(), ch.getSummary(), lessons
        );
    }

    /** Dùng khi tạo lesson để trả DTO mỏng. */
    public static LessonRes toLessonRes(Lesson ls) {
        return new LessonRes(
                ls.getId(), ls.getTitle(), ls.getOrderIndex(), ls.getTotalDurationSec(), List.of()
        );
    }

    public static LessonRes toLessonResDeep(Lesson ls) {
        List<SectionRes> sections = Hibernate.isInitialized(ls.getSections())
                ? ls.getSections().stream().map(CourseMapper::toSectionResDeep).collect(Collectors.toList())
                : List.of();
        return new LessonRes(
                ls.getId(), ls.getTitle(), ls.getOrderIndex(), ls.getTotalDurationSec(), sections
        );
    }

    /** Dùng khi tạo section để trả DTO mỏng. */
    public static SectionRes toSectionRes(Section sc) {
        return new SectionRes(
                sc.getId(), sc.getTitle(), sc.getOrderIndex(), sc.getStudyType(),
                sc.getFlashcardSetId(), List.of()
        );
    }

    public static SectionRes toSectionResDeep(Section sc) {
        List<ContentRes> contents = Hibernate.isInitialized(sc.getContents())
                ? sc.getContents().stream().map(CourseMapper::toContentRes).collect(Collectors.toList())
                : List.of();
        return new SectionRes(
                sc.getId(), sc.getTitle(), sc.getOrderIndex(), sc.getStudyType(),
                sc.getFlashcardSetId(), contents
        );
    }

    public static ContentRes toContentRes(SectionsContent ct) {
        return new ContentRes(
                ct.getId(), ct.getOrderIndex(), ct.getContentFormat(), ct.isPrimaryContent(),
                ct.getFilePath(), ct.getRichText(), ct.getQuizId(), ct.getFlashcardSetId()
        );
    }

    /* =======================
       Private helpers
       ======================= */

    private static List<ChapterRes> mapChaptersDeepSafe(Course c) {
        if (!Hibernate.isInitialized(c.getChapters())) return List.of();
        return c.getChapters().stream()
                .map(CourseMapper::toChapterResDeep)
                .collect(Collectors.toList());
    }
}
