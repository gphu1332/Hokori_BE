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
        CourseRes res = new CourseRes();
        res.setId(c.getId());
        res.setTitle(c.getTitle());
        res.setSlug(c.getSlug());
        res.setSubtitle(c.getSubtitle());
        res.setDescription(c.getDescription());
        res.setLevel(c.getLevel());
        res.setPriceCents(c.getPriceCents());
        res.setDiscountedPriceCents(c.getDiscountedPriceCents());
        res.setCurrency(c.getCurrency());
        res.setCoverImagePath(c.getCoverImagePath());
        res.setStatus(c.getStatus());
        res.setPublishedAt(c.getPublishedAt());
        res.setUserId(c.getUserId());
        // teacherName sẽ được set ở service nếu cần
        res.setTeacherName(null);
        // nếu CourseRes có field enrollCount thì set luôn, không thì bỏ dòng này
        res.setEnrollCount(c.getEnrollCount());
        // map tree
        res.setChapters(mapChaptersDeepSafe(c));
        return res;
    }


    /** Trả course chỉ với đúng 1 chapter (ví dụ: chapter học thử). */
    public static CourseRes toResOnlyTrial(Course c, Chapter trial) {
        CourseRes res = new CourseRes();
        res.setId(c.getId());
        res.setTitle(c.getTitle());
        res.setSlug(c.getSlug());
        res.setSubtitle(c.getSubtitle());
        res.setDescription(c.getDescription());
        res.setLevel(c.getLevel());
        res.setPriceCents(c.getPriceCents());
        res.setDiscountedPriceCents(c.getDiscountedPriceCents());
        res.setCurrency(c.getCurrency());
        res.setCoverImagePath(c.getCoverImagePath());
        res.setStatus(c.getStatus());
        res.setPublishedAt(c.getPublishedAt());
        res.setUserId(c.getUserId());
        res.setTeacherName(null);
        res.setEnrollCount(c.getEnrollCount());
        res.setChapters(List.of(toChapterResDeep(trial)));
        return res;
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
                ct.getId(),
                ct.getOrderIndex(),
                ct.getContentFormat(),
                ct.isPrimaryContent(),
                ct.getFilePath(),
                ct.getRichText(),
                ct.getFlashcardSetId()
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
