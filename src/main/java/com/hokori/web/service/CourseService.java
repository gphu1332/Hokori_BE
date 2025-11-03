package com.hokori.web.service;

import com.hokori.web.Enum.ContentFormat;
import com.hokori.web.Enum.ContentType;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.dto.course.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import com.hokori.web.util.SlugUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.*;

/*
 * REPO cần có:
 * ChapterRepository:
 *   long countByCourse_Id(Long courseId);
 *   long countByCourse_IdAndIsTrialTrue(Long courseId);
 *   Optional<Chapter> findByCourse_IdAndIsTrialTrue(Long courseId);
 *
 * LessonRepository:  long countByChapter_Id(Long chapterId);
 * SectionRepository: long countByLesson_Id(Long lessonId);
 * SectionsContentRepository: long countBySection_Id(Long sectionId);
 */

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final LessonRepository lessonRepo;
    private final SectionRepository sectionRepo;
    private final SectionsContentRepository contentRepo;

    // ===== Course =====

    public CourseRes createCourse(Long teacherUserId, @Valid CourseUpsertReq r) {
        Course c = new Course();
        c.setUserId(teacherUserId);
        applyCourse(c, r);
        c.setSlug(uniqueSlug(r.getTitle()));
        c = courseRepo.save(c);

        // Auto tạo 1 Chapter học thử ở orderIndex = 0
        Chapter preview = new Chapter();
        preview.setCourse(c);
        preview.setTitle("Học thử");
        preview.setSummary("Nội dung dùng thử miễn phí");
        preview.setOrderIndex(0);
        preview.setTrial(true);
        chapterRepo.save(preview);

        return CourseMapper.toRes(c);
    }

    public CourseRes updateCourse(Long id, Long teacherUserId, @Valid CourseUpsertReq r) {
        Course c = getOwned(id, teacherUserId);
        String old = c.getSlug();
        applyCourse(c, r);
        if (!SlugUtil.toSlug(r.getTitle()).equals(old)) {
            c.setSlug(uniqueSlug(r.getTitle()));
        }
        return CourseMapper.toRes(c);
    }

    public void softDelete(Long id, Long teacherUserId) {
        getOwned(id, teacherUserId).setDeletedFlag(true);
    }

    public CourseRes publish(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);

        // đúng 1 chapter học thử
        long trialCount = chapterRepo.countByCourse_IdAndIsTrialTrue(id);
        if (trialCount != 1) throw bad("Course must have exactly ONE trial chapter");

        // validate toàn bộ tree
        c.getChapters().forEach(ch ->
                ch.getLessons().forEach(ls ->
                        ls.getSections().forEach(this::validateSectionBeforePublish)
                )
        );

        c.setStatus(CourseStatus.PUBLISHED);
        c.setPublishedAt(Instant.now());
        return CourseMapper.toRes(c);
    }

    public CourseRes unpublish(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);
        c.setStatus(CourseStatus.DRAFT);
        c.setPublishedAt(null);
        return CourseMapper.toRes(c);
    }

    @Transactional(readOnly = true)
    public CourseRes getTree(Long id) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return CourseMapper.toRes(c);
    }

    /** Trả về chỉ cây của chapter học thử (làm endpoint public /trial) */
    @Transactional(readOnly = true)
    public CourseRes getTrialTree(Long courseId) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        Chapter trial = chapterRepo.findByCourse_IdAndIsTrialTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No trial chapter"));
        return CourseMapper.toResOnlyTrial(c, trial);
    }

    @Transactional(readOnly = true)
    public Page<CourseRes> listMine(Long teacherUserId, int page, int size, String q, CourseStatus status) {
        Pageable p = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Specification<Course> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("deletedFlag")));
            ps.add(cb.equal(root.get("userId"), teacherUserId));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("slug")), like)
                ));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return courseRepo.findAll(spec, p).map(CourseMapper::toRes);
    }

    @Transactional(readOnly = true)
    public Page<CourseRes> listPublished(JLPTLevel level, int page, int size) {
        return courseRepo.findPublishedByLevel(
                level,
                PageRequest.of(page, size, Sort.by("publishedAt").descending())
        ).map(CourseMapper::toRes);
    }

    private void applyCourse(Course c, CourseUpsertReq r) {
        c.setTitle(r.getTitle());
        c.setSubtitle(r.getSubtitle());
        c.setDescription(r.getDescription());
        c.setLevel(r.getLevel() == null ? JLPTLevel.N5 : r.getLevel());
        c.setPriceCents(r.getPriceCents());
        c.setDiscountedPriceCents(r.getDiscountedPriceCents());
        if (r.getCurrency() != null) c.setCurrency(r.getCurrency());
        c.setCoverAssetId(r.getCoverAssetId());
    }

    private String uniqueSlug(String title) {
        String base = SlugUtil.toSlug(title);
        String s = base;
        int i = 1;
        while (courseRepo.findBySlugAndDeletedFlagFalse(s).isPresent()) {
            s = base + "-" + (++i);
        }
        return s;
    }

    private Course getOwned(Long id, Long teacherUserId) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!Objects.equals(c.getUserId(), teacherUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
        }
        return c;
    }

    // ===== Children (trả DTO để tránh LAZY) =====

// ===== Children (return DTO only, no overloads) =====

    public ChapterRes createChapter(Long courseId, Long teacherUserId, ChapterUpsertReq r) {
        Course c = getOwned(courseId, teacherUserId);

        Chapter ch = new Chapter();
        ch.setCourse(c);
        ch.setTitle(r.getTitle());
        ch.setSummary(r.getSummary());

        // Append nếu không truyền orderIndex
        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(chapterRepo.countByCourse_Id(courseId))
                : r.getOrderIndex();
        ch.setOrderIndex(oi);

        // isTrial: chỉ được 1 chapter học thử
        if (Boolean.TRUE.equals(r.getIsTrial())) {
            if (chapterRepo.countByCourse_IdAndIsTrialTrue(courseId) > 0) {
                throw bad("Course already has a trial chapter");
            }
            ch.setTrial(true);
        }

        Chapter saved = chapterRepo.save(ch);
        return toChapterRes(saved);
    }

    public LessonRes createLesson(Long chapterId, Long teacherUserId, LessonUpsertReq r) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        // check owner
        getOwned(ch.getCourse().getId(), teacherUserId);

        Lesson ls = new Lesson();
        ls.setChapter(ch);
        ls.setTitle(r.getTitle());

        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(lessonRepo.countByChapter_Id(chapterId))
                : r.getOrderIndex();
        ls.setOrderIndex(oi);

        ls.setTotalDurationSec(r.getTotalDurationSec() == null ? 0L : r.getTotalDurationSec());

        Lesson saved = lessonRepo.save(ls);
        return toLessonRes(saved);
    }

    public SectionRes createSection(Long lessonId, Long teacherUserId, SectionUpsertReq r) {
        Lesson ls = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        // check owner
        getOwned(ls.getChapter().getCourse().getId(), teacherUserId);

        Section s = new Section();
        s.setLesson(ls);
        s.setTitle(r.getTitle());

        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(sectionRepo.countByLesson_Id(lessonId))
                : r.getOrderIndex();
        s.setOrderIndex(oi);

        s.setStudyType(r.getStudyType() == null ? ContentType.GRAMMAR : r.getStudyType());
        s.setFlashcardSetId(r.getFlashcardSetId());

        // VOCAB phải có flashcardSetId
        validateSectionByStudyType(s);

        Section saved = sectionRepo.save(s);
        return toSectionRes(saved);
    }

    public ContentRes createContent(Long sectionId, Long teacherUserId, ContentUpsertReq r) {
        Section sc = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        // check owner
        getOwned(sc.getLesson().getChapter().getCourse().getId(), teacherUserId);

        // Ràng buộc payload theo contentFormat & studyType
        validateContentPayload(sc, r);

        SectionsContent ct = new SectionsContent();
        ct.setSection(sc);

        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(contentRepo.countBySection_Id(sectionId))
                : r.getOrderIndex();
        ct.setOrderIndex(oi);

        ct.setContentFormat(r.getContentFormat() == null ? ContentFormat.ASSET : r.getContentFormat());
        ct.setPrimaryContent(r.isPrimaryContent());
        ct.setAssetId(r.getAssetId());
        ct.setRichText(r.getRichText());
        ct.setQuizId(r.getQuizId());
        ct.setFlashcardSetId(r.getFlashcardSetId());

        SectionsContent saved = contentRepo.save(ct);
        return toContentRes(saved);
    }

// ===== Lightweight mappers (local) =====

    private ChapterRes toChapterRes(Chapter ch) {
        return new ChapterRes(
                ch.getId(),
                ch.getTitle(),
                ch.getOrderIndex(),
                ch.getSummary(),
                ch.getLessons() == null ? List.of() :
                        ch.getLessons().stream().sorted(Comparator.comparing(Lesson::getOrderIndex))
                                .map(this::toLessonRes).toList()
        );
    }

    private LessonRes toLessonRes(Lesson ls) {
        return new LessonRes(
                ls.getId(),
                ls.getTitle(),
                ls.getOrderIndex(),
                ls.getTotalDurationSec(),
                ls.getSections() == null ? List.of() :
                        ls.getSections().stream().sorted(Comparator.comparing(Section::getOrderIndex))
                                .map(this::toSectionRes).toList()
        );
    }

    private SectionRes toSectionRes(Section s) {
        return new SectionRes(
                s.getId(),
                s.getTitle(),
                s.getOrderIndex(),
                s.getStudyType(),
                s.getFlashcardSetId(),
                s.getContents() == null ? List.of() :
                        s.getContents().stream().sorted(Comparator.comparing(SectionsContent::getOrderIndex))
                                .map(this::toContentRes).toList()
        );
    }

    private ContentRes toContentRes(SectionsContent c) {
        return new ContentRes(
                c.getId(),
                c.getOrderIndex(),
                c.getContentFormat(),
                c.isPrimaryContent(),
                c.getAssetId(),
                c.getRichText(),
                c.getQuizId(),
                c.getFlashcardSetId()
        );
    }

    // ===== Trial helpers =====

    /** Đánh dấu/chuyển chapter học thử. Gỡ cờ cái cũ (nếu khác) rồi set cho chapterId truyền vào. */
    public ChapterRes markTrialChapter(Long chapterId, Long teacherUserId) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        Course c = getOwned(ch.getCourse().getId(), teacherUserId);

        chapterRepo.findByCourse_IdAndIsTrialTrue(c.getId()).ifPresent(old -> {
            if (!old.getId().equals(ch.getId())) old.setTrial(false);
        });

        ch.setTrial(true);
        return CourseMapper.toChapterRes(ch);
    }

    // ===== Rules & validate =====

    private void validateSectionByStudyType(Section s) {
        if (s.getStudyType() == ContentType.VOCABULARY && s.getFlashcardSetId() == null) {
            throw bad("VOCABULARY section requires flashcardSetId");
        }
    }

    private void validateContentPayload(Section section, ContentUpsertReq r) {
        ContentFormat fmt = r.getContentFormat();
        if (fmt == null || fmt == ContentFormat.ASSET) {
            if (r.getAssetId() == null) throw bad("assetId is required for ASSET");

            if (section.getStudyType() == ContentType.GRAMMAR && r.isPrimaryContent()) {
                long primaryCount = section.getContents().stream()
                        .filter(SectionsContent::isPrimaryContent).count();
                if (primaryCount >= 1) throw bad("GRAMMAR requires exactly ONE primary video (already set)");
            }

        } else if (fmt == ContentFormat.RICH_TEXT) {
            if (r.getRichText() == null || r.getRichText().isBlank()) throw bad("richText is required for RICH_TEXT");
            if (r.isPrimaryContent()) throw bad("primaryContent must be false for RICH_TEXT");

        } else if (fmt == ContentFormat.FLASHCARD_SET) {
            if (section.getStudyType() != ContentType.VOCABULARY)
                throw bad("FLASHCARD_SET only allowed in VOCAB sections");
            if (r.getFlashcardSetId() == null) throw bad("flashcardSetId required for FLASHCARD_SET");

        } else if (fmt == ContentFormat.QUIZ_REF) {
            if (r.getQuizId() == null) throw bad("quizId required for QUIZ_REF");
        }
    }

    private void validateSectionBeforePublish(Section s) {
        if (s.getStudyType() == ContentType.VOCABULARY) {
            if (s.getFlashcardSetId() == null) throw bad("VOCAB section must link a flashcard set");

        } else if (s.getStudyType() == ContentType.GRAMMAR) {
            long primary = s.getContents().stream().filter(SectionsContent::isPrimaryContent).count();
            if (primary != 1) throw bad("GRAMMAR section requires exactly ONE primary video");

        } else if (s.getStudyType() == ContentType.KANJI) {
            long primary = s.getContents().stream().filter(SectionsContent::isPrimaryContent).count();
            if (primary < 1) throw bad("KANJI section requires at least ONE primary content (video or doc)");
        }
    }

    private ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }
}
