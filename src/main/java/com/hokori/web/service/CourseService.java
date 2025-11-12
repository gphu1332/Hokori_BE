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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final LessonRepository lessonRepo;
    private final SectionRepository sectionRepo;
    private final SectionsContentRepository contentRepo;

    // =========================
    // COURSE
    // =========================
    public CourseRes createCourse(Long teacherUserId, @Valid CourseUpsertReq r) {
        Course c = new Course();
        c.setUserId(teacherUserId);
        applyCourse(c, r);
        c.setSlug(uniqueSlug(r.getTitle()));
        c = courseRepo.save(c);

        // Tự tạo 1 chapter học thử
        Chapter preview = new Chapter();
        preview.setCourse(c);
        preview.setTitle("Học thử");
        preview.setSummary("Nội dung dùng thử miễn phí");
        preview.setOrderIndex(0);
        preview.setTrial(true);
        chapterRepo.save(preview);

        return toCourseResLite(c);
    }

    public CourseRes updateCourse(Long id, Long teacherUserId, @Valid CourseUpsertReq r) {
        Course c = getOwned(id, teacherUserId);
        String old = c.getSlug();
        applyCourse(c, r);
        if (!SlugUtil.toSlug(r.getTitle()).equals(old)) {
            c.setSlug(uniqueSlug(r.getTitle()));
        }
        return toCourseResLite(c);
    }

    public void softDelete(Long id, Long teacherUserId) {
        getOwned(id, teacherUserId).setDeletedFlag(true);
    }

    public CourseRes publish(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);

        // Đúng 1 chapter học thử
        long trialCount = chapterRepo.countByCourse_IdAndIsTrialTrue(id);
        if (trialCount != 1) throw bad("Course must have exactly ONE trial chapter");

        // Validate cấu trúc
        c.getChapters().forEach(ch ->
                ch.getLessons().forEach(ls ->
                        ls.getSections().forEach(this::validateSectionBeforePublish)
                )
        );

        c.setStatus(CourseStatus.PUBLISHED);
        c.setPublishedAt(Instant.now());
        return toCourseResLite(c);
    }

    public CourseRes unpublish(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);
        c.setStatus(CourseStatus.DRAFT);
        c.setPublishedAt(null);
        return toCourseResLite(c);
    }

    @Transactional(readOnly = true)
    public CourseRes getTree(Long id) {
        var c = courseRepo.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        var chapterEntities = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(c.getId());
        var chapterDtos = new java.util.ArrayList<ChapterRes>();

        for (var ch : chapterEntities) {
            var lessonEntities = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(ch.getId());
            var lessonDtos = new java.util.ArrayList<LessonRes>();

            for (var ls : lessonEntities) {
                var sectionEntities = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(ls.getId());
                var sectionDtos = new java.util.ArrayList<SectionRes>();

                for (var s : sectionEntities) {
                    var contentEntities = contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId());
                    var contentDtos = new java.util.ArrayList<ContentRes>(contentEntities.size());
                    for (var ct : contentEntities) {
                        contentDtos.add(new ContentRes(
                                ct.getId(), ct.getOrderIndex(), ct.getContentFormat(), ct.isPrimaryContent(),
                                ct.getAssetId(), ct.getRichText(), ct.getQuizId(), ct.getFlashcardSetId()
                        ));
                    }
                    sectionDtos.add(new SectionRes(
                            s.getId(), s.getTitle(), s.getOrderIndex(), s.getStudyType(), s.getFlashcardSetId(), contentDtos
                    ));
                }

                lessonDtos.add(new LessonRes(
                        ls.getId(), ls.getTitle(), ls.getOrderIndex(), ls.getTotalDurationSec(), sectionDtos
                ));
            }

            chapterDtos.add(new ChapterRes(
                    ch.getId(), ch.getTitle(), ch.getOrderIndex(), ch.getSummary(), lessonDtos
            ));
        }

        return new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(),
                c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(), c.getCoverAssetId(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                chapterDtos
        );
    }


    @Transactional(readOnly = true)
    public CourseRes getTrialTree(Long courseId) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        Chapter trial = chapterRepo.findByCourse_IdAndIsTrialTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No trial chapter"));
        return toCourseResWithChapters(c, List.of(trial));
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
                ps.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("slug")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return courseRepo.findAll(spec, p).map(this::toCourseResLite);
    }

    @Transactional(readOnly = true)
    public Page<CourseRes> listPublished(JLPTLevel level, int page, int size) {
        return courseRepo.findPublishedByLevel(
                level, PageRequest.of(page, size, Sort.by("publishedAt").descending())
        ).map(this::toCourseResLite);
    }

    // =========================
    // CHILDREN (trả DTO, tránh fetch tree)
    // =========================

    public ChapterRes createChapter(Long courseId, Long teacherUserId, ChapterUpsertReq r) {
        assertOwner(courseId, teacherUserId);

        Course courseRef = courseRepo.getReferenceById(courseId);

        Chapter ch = new Chapter();
        ch.setCourse(courseRef);
        ch.setTitle(r.getTitle());
        ch.setSummary(r.getSummary());

        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(chapterRepo.countByCourse_Id(courseId))
                : r.getOrderIndex();
        ch.setOrderIndex(oi);

        if (Boolean.TRUE.equals(r.getIsTrial())) {
            if (chapterRepo.countByCourse_IdAndIsTrialTrue(courseId) > 0) {
                throw bad("Course already has a trial chapter");
            }
            ch.setTrial(true);
        }

        Chapter saved = chapterRepo.save(ch);
        return toChapterResShallow(saved);
    }

    public LessonRes createLesson(Long chapterId, Long teacherUserId, LessonUpsertReq r) {
        Long courseId = chapterRepo.findCourseIdByChapterId(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        assertOwner(courseId, teacherUserId);

        Lesson ls = new Lesson();
        ls.setChapter(chapterRepo.getReferenceById(chapterId));
        ls.setTitle(r.getTitle());

        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(lessonRepo.countByChapter_Id(chapterId))
                : r.getOrderIndex();
        ls.setOrderIndex(oi);
        ls.setTotalDurationSec(r.getTotalDurationSec() == null ? 0L : r.getTotalDurationSec());

        Lesson saved = lessonRepo.save(ls);
        return toLessonResShallow(saved);
    }

    public SectionRes createSection(Long lessonId, Long teacherUserId, SectionUpsertReq r) {
        Long courseId = lessonRepo.findCourseIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        assertOwner(courseId, teacherUserId);

        Section s = new Section();
        s.setLesson(lessonRepo.getReferenceById(lessonId));
        s.setTitle(r.getTitle());

        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(sectionRepo.countByLesson_Id(lessonId))
                : r.getOrderIndex();
        s.setOrderIndex(oi);

        s.setStudyType(r.getStudyType() == null ? ContentType.GRAMMAR : r.getStudyType());
        s.setFlashcardSetId(r.getFlashcardSetId());
        validateSectionByStudyType(s);

        Section saved = sectionRepo.save(s);
        return toSectionResShallow(saved);
    }

    public ContentRes createContent(Long sectionId, Long teacherUserId, ContentUpsertReq r) {
        Long courseId = sectionRepo.findCourseIdBySectionId(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        assertOwner(courseId, teacherUserId);

        Section scRef = sectionRepo.getReferenceById(sectionId);
        validateContentPayload(scRef, r);

        SectionsContent ct = new SectionsContent();
        ct.setSection(scRef);

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

    public ChapterRes markTrialChapter(Long chapterId, Long teacherUserId) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        assertOwner(ch.getCourse().getId(), teacherUserId);

        chapterRepo.findByCourse_IdAndIsTrialTrue(ch.getCourse().getId()).ifPresent(old -> {
            if (!old.getId().equals(ch.getId())) old.setTrial(false);
        });

        ch.setTrial(true);
        return toChapterResShallow(ch);
    }

    // =========================
    // VALIDATION & HELPERS
    // =========================

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
            if (r.getRichText() == null || r.getRichText().isBlank())
                throw bad("richText is required for RICH_TEXT");
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

    private void assertOwner(Long courseId, Long teacherUserId) {
        if (!courseRepo.existsByIdAndUserIdAndDeletedFlagFalse(courseId, teacherUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
        }
    }

    private ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    // =========================
    // MAPPERS
    // =========================

    private CourseRes toCourseResLite(Course c) {
        return new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(),
                c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(), c.getCoverAssetId(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                List.of()
        );
    }

    private CourseRes toCourseResFull(Course c) {
        List<ChapterRes> chapters = c.getChapters().stream()
                .sorted(Comparator.comparing(Chapter::getOrderIndex))
                .map(ch -> new ChapterRes(
                        ch.getId(), ch.getTitle(), ch.getOrderIndex(), ch.getSummary(),
                        ch.getLessons().stream()
                                .sorted(Comparator.comparing(Lesson::getOrderIndex))
                                .map(ls -> new LessonRes(
                                        ls.getId(), ls.getTitle(), ls.getOrderIndex(), ls.getTotalDurationSec(),
                                        ls.getSections().stream()
                                                .sorted(Comparator.comparing(Section::getOrderIndex))
                                                .map(this::toSectionResFull)
                                                .collect(Collectors.toList())
                                )).collect(Collectors.toList())
                )).collect(Collectors.toList());

        return new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(),
                c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(), c.getCoverAssetId(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                chapters
        );
    }

    private CourseRes toCourseResWithChapters(Course c, List<Chapter> include) {
        List<ChapterRes> chapters = include.stream()
                .sorted(Comparator.comparing(Chapter::getOrderIndex))
                .map(ch -> new ChapterRes(
                        ch.getId(), ch.getTitle(), ch.getOrderIndex(), ch.getSummary(),
                        ch.getLessons().stream()
                                .sorted(Comparator.comparing(Lesson::getOrderIndex))
                                .map(this::toLessonResFull)
                                .collect(Collectors.toList())
                )).collect(Collectors.toList());

        return new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(),
                c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(), c.getCoverAssetId(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                chapters
        );
    }

    private ChapterRes toChapterResShallow(Chapter ch) {
        return new ChapterRes(ch.getId(), ch.getTitle(), ch.getOrderIndex(), ch.getSummary(), List.of());
    }

    private LessonRes toLessonResShallow(Lesson ls) {
        return new LessonRes(ls.getId(), ls.getTitle(), ls.getOrderIndex(), ls.getTotalDurationSec(), List.of());
    }

    private SectionRes toSectionResShallow(Section s) {
        return new SectionRes(s.getId(), s.getTitle(), s.getOrderIndex(), s.getStudyType(), s.getFlashcardSetId(), List.of());
    }

    private SectionRes toSectionResFull(Section s) {
        List<ContentRes> contents = s.getContents().stream()
                .sorted(Comparator.comparing(SectionsContent::getOrderIndex))
                .map(this::toContentRes)
                .collect(Collectors.toList());
        return new SectionRes(s.getId(), s.getTitle(), s.getOrderIndex(), s.getStudyType(), s.getFlashcardSetId(), contents);
    }

    private LessonRes toLessonResFull(Lesson ls) {
        List<SectionRes> sections = ls.getSections().stream()
                .sorted(Comparator.comparing(Section::getOrderIndex))
                .map(this::toSectionResFull)
                .collect(Collectors.toList());
        return new LessonRes(ls.getId(), ls.getTitle(), ls.getOrderIndex(), ls.getTotalDurationSec(), sections);
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

    // CourseService.java  (bổ sung vào class hiện tại)

    // =========================
    // COURSE DETAIL (metadata only)
    // =========================
    @Transactional(readOnly = true)
    public CourseRes getDetail(Long id, Long teacherUserId) {
        // Check if we're using PostgreSQL (which has LOB stream issues)
        // SQL Server doesn't have this issue, so we can use entity directly
        boolean isPostgreSQL = isPostgreSQLDatabase();
        
        if (isPostgreSQL) {
            // Use native query to avoid LOB stream error (PostgreSQL only)
            var metadataOpt = courseRepo.findCourseMetadataById(id);
            if (metadataOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
            }
            
            Object[] metadata = metadataOpt.get();
            
            // Verify ownership
            Long courseUserId = null;
            Object userIdObj = metadata[11]; // user_id is at index 11
            if (userIdObj instanceof Number) {
                courseUserId = ((Number) userIdObj).longValue();
            } else if (userIdObj != null) {
                try {
                    courseUserId = Long.parseLong(userIdObj.toString());
                } catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid user_id in course");
                }
            }
            
            if (!Objects.equals(courseUserId, teacherUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
            }
            
            // Build CourseRes from metadata (without description to avoid LOB)
            return buildCourseResFromMetadata(metadata);
        } else {
            // SQL Server: Use entity directly (no LOB stream issues)
            Course c = getOwned(id, teacherUserId);
            return toCourseResLite(c);
        }
    }
    
    /**
     * Detect if current database is PostgreSQL.
     * SQL Server doesn't have LOB stream issues, so we can use entity directly.
     */
    private boolean isPostgreSQLDatabase() {
        try {
            // Check active profile first (most reliable)
            String activeProfile = System.getProperty("spring.profiles.active", 
                    System.getenv("SPRING_PROFILES_ACTIVE"));
            if ("prod".equals(activeProfile)) {
                return true; // Production uses PostgreSQL on Railway
            }
            
            // Check DATABASE_URL (Railway PostgreSQL)
            String databaseUrl = System.getenv("DATABASE_URL");
            if (databaseUrl != null && (databaseUrl.contains("postgresql://") || databaseUrl.contains("postgres://"))) {
                return true;
            }
            
            // Check JDBC URL from system properties
            String datasourceUrl = System.getProperty("spring.datasource.url");
            if (datasourceUrl != null) {
                return datasourceUrl.contains("postgresql") || datasourceUrl.contains("postgres");
            }
            
            // Default: assume dev (SQL Server) unless explicitly PostgreSQL
            return false;
        } catch (Exception e) {
            // If detection fails, default to false (SQL Server) for safety
            return false;
        }
    }
    
    private CourseRes buildCourseResFromMetadata(Object[] metadata) {
        // [id, title, slug, subtitle, level, priceCents, discountedPriceCents, currency, coverAssetId, status, publishedAt, userId, deletedFlag]
        Long id = extractLong(metadata[0]);
        String title = metadata[1] != null ? metadata[1].toString() : null;
        String slug = metadata[2] != null ? metadata[2].toString() : null;
        String subtitle = metadata[3] != null ? metadata[3].toString() : null;
        
        // Level enum
        com.hokori.web.Enum.JLPTLevel level = com.hokori.web.Enum.JLPTLevel.N5;
        if (metadata[4] != null) {
            try {
                level = com.hokori.web.Enum.JLPTLevel.valueOf(metadata[4].toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                level = com.hokori.web.Enum.JLPTLevel.N5;
            }
        }
        
        Long priceCents = extractLong(metadata[5]);
        Long discountedPriceCents = extractLong(metadata[6]);
        String currency = metadata[7] != null ? metadata[7].toString() : "VND";
        Long coverAssetId = extractLong(metadata[8]);
        
        // Status enum
        com.hokori.web.Enum.CourseStatus status = com.hokori.web.Enum.CourseStatus.DRAFT;
        if (metadata[9] != null) {
            try {
                status = com.hokori.web.Enum.CourseStatus.valueOf(metadata[9].toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                status = com.hokori.web.Enum.CourseStatus.DRAFT;
            }
        }
        
        // PublishedAt (Instant)
        java.time.Instant publishedAt = null;
        if (metadata[10] != null) {
            try {
                if (metadata[10] instanceof java.sql.Timestamp) {
                    publishedAt = ((java.sql.Timestamp) metadata[10]).toInstant();
                } else if (metadata[10] instanceof java.time.Instant) {
                    publishedAt = (java.time.Instant) metadata[10];
                } else {
                    // Try to parse as string
                    publishedAt = java.time.Instant.parse(metadata[10].toString());
                }
            } catch (Exception e) {
                // Ignore, keep as null
            }
        }
        
        Long userId = extractLong(metadata[11]);
        
        return new CourseRes(
                id, 
                title, 
                slug, 
                subtitle,
                null, // description = null (avoid LOB, can be loaded separately if needed)
                level,
                priceCents, 
                discountedPriceCents, 
                currency, 
                coverAssetId,
                status, 
                publishedAt, 
                userId,
                Collections.emptyList() // Empty chapters list for lite version
        );
    }
    
    private Long extractLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // =========================
    // CHAPTER: update / delete / reorder
    // =========================
    public ChapterRes updateChapter(Long chapterId, Long teacherUserId, ChapterUpsertReq r) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        assertOwner(ch.getCourse().getId(), teacherUserId);

        if (r.getTitle() != null) ch.setTitle(r.getTitle());
        ch.setSummary(r.getSummary());
        if (Boolean.TRUE.equals(r.getIsTrial())) {
            // set trial duy nhất trong course
            chapterRepo.findByCourse_IdAndIsTrialTrue(ch.getCourse().getId()).ifPresent(old -> {
                if (!old.getId().equals(ch.getId())) old.setTrial(false);
            });
            ch.setTrial(true);
        }
        return toChapterResShallow(ch);
    }

    public void deleteChapter(Long chapterId, Long teacherUserId) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        assertOwner(ch.getCourse().getId(), teacherUserId);

        Long courseId = ch.getCourse().getId();
        chapterRepo.delete(ch);
        renormalizeChapterOrder(courseId);
    }

    public ChapterRes reorderChapter(Long chapterId, Long teacherUserId, int newIndex) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        assertOwner(ch.getCourse().getId(), teacherUserId);

        List<Chapter> list = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(ch.getCourse().getId());
        applyReorder(list, chapterId, newIndex, (it, idx) -> it.setOrderIndex(idx));
        return toChapterResShallow(ch);
    }

    // =========================
    // LESSON: update / delete / reorder
    // =========================
    public LessonRes updateLesson(Long lessonId, Long teacherUserId, LessonUpsertReq r) {
        Lesson ls = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        assertOwner(ls.getChapter().getCourse().getId(), teacherUserId);

        if (r.getTitle() != null) ls.setTitle(r.getTitle());
        if (r.getTotalDurationSec() != null) ls.setTotalDurationSec(r.getTotalDurationSec());
        return toLessonResShallow(ls);
    }

    public void deleteLesson(Long lessonId, Long teacherUserId) {
        Lesson ls = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        Long courseId = ls.getChapter().getCourse().getId();
        assertOwner(courseId, teacherUserId);

        Long chapterId = ls.getChapter().getId();
        lessonRepo.delete(ls);
        renormalizeLessonOrder(chapterId);
    }

    public LessonRes reorderLesson(Long lessonId, Long teacherUserId, int newIndex) {
        Lesson ls = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        assertOwner(ls.getChapter().getCourse().getId(), teacherUserId);

        List<Lesson> list = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(ls.getChapter().getId());
        applyReorder(list, lessonId, newIndex, (it, idx) -> it.setOrderIndex(idx));
        return toLessonResShallow(ls);
    }

    // =========================
    // SECTION: update / delete / reorder
    // =========================
    public SectionRes updateSection(Long sectionId, Long teacherUserId, SectionUpsertReq r) {
        Section s = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        assertOwner(s.getLesson().getChapter().getCourse().getId(), teacherUserId);

        if (r.getTitle() != null) s.setTitle(r.getTitle());
        if (r.getStudyType() != null) s.setStudyType(r.getStudyType());
        if (r.getFlashcardSetId() != null) s.setFlashcardSetId(r.getFlashcardSetId());
        validateSectionByStudyType(s);

        return toSectionResShallow(s);
    }

    public void deleteSection(Long sectionId, Long teacherUserId) {
        Section s = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        Long courseId = s.getLesson().getChapter().getCourse().getId();
        assertOwner(courseId, teacherUserId);

        Long lessonId = s.getLesson().getId();
        sectionRepo.delete(s);
        renormalizeSectionOrder(lessonId);
    }

    public SectionRes reorderSection(Long sectionId, Long teacherUserId, int newIndex) {
        Section s = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        assertOwner(s.getLesson().getChapter().getCourse().getId(), teacherUserId);

        List<Section> list = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(s.getLesson().getId());
        applyReorder(list, sectionId, newIndex, (it, idx) -> it.setOrderIndex(idx));
        return toSectionResShallow(s);
    }

    // =========================
    // CONTENT: update / delete / reorder
    // =========================
    public ContentRes updateContent(Long contentId, Long teacherUserId, ContentUpsertReq r) {
        SectionsContent c = contentRepo.findById(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));

        Long courseId = c.getSection().getLesson().getChapter().getCourse().getId();
        assertOwner(courseId, teacherUserId);

        // validate theo section hiện tại
        validateContentPayload(c.getSection(), r);

        if (r.getContentFormat() != null) c.setContentFormat(r.getContentFormat());
        c.setPrimaryContent(r.isPrimaryContent());
        c.setAssetId(r.getAssetId());
        c.setRichText(r.getRichText());
        c.setQuizId(r.getQuizId());
        c.setFlashcardSetId(r.getFlashcardSetId());

        return toContentRes(c);
    }

    public void deleteContent(Long contentId, Long teacherUserId) {
        SectionsContent c = contentRepo.findById(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        Long courseId = c.getSection().getLesson().getChapter().getCourse().getId();
        assertOwner(courseId, teacherUserId);

        Long sectionId = c.getSection().getId();
        contentRepo.delete(c);
        renormalizeContentOrder(sectionId);
    }

    public ContentRes reorderContent(Long contentId, Long teacherUserId, int newIndex) {
        SectionsContent c = contentRepo.findById(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        assertOwner(c.getSection().getLesson().getChapter().getCourse().getId(), teacherUserId);

        List<SectionsContent> list = contentRepo.findBySection_IdOrderByOrderIndexAsc(c.getSection().getId());
        applyReorder(list, contentId, newIndex, (it, idx) -> it.setOrderIndex(idx));
        return toContentRes(c);
    }

    // =========================
    // REORDER HELPERS
    // =========================
    private <T> void applyReorder(List<T> ordered,
                                  Long targetId,
                                  int newIndex,
                                  java.util.function.BiConsumer<T, Integer> setIndex) {
        if (ordered.isEmpty()) return;

        // clamp
        int max = ordered.size() - 1;
        int idx = Math.max(0, Math.min(newIndex, max));

        // tách target
        T target = null;
        Iterator<T> it = ordered.iterator();
        while (it.hasNext()) {
            T cur = it.next();
            Long id = extractId(cur);
            if (Objects.equals(id, targetId)) { target = cur; it.remove(); break; }
        }
        if (target == null) throw bad("Target not in list");

        ordered.add(idx, target);
        for (int i = 0; i < ordered.size(); i++) setIndex.accept(ordered.get(i), i);
    }

    private Long extractId(Object o) {
        if (o instanceof Chapter ch) return ch.getId();
        if (o instanceof Lesson ls) return ls.getId();
        if (o instanceof Section s) return s.getId();
        if (o instanceof SectionsContent c) return c.getId();
        return null;
    }

    private void renormalizeChapterOrder(Long courseId) {
        List<Chapter> list = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
        for (int i = 0; i < list.size(); i++) list.get(i).setOrderIndex(i);
    }
    private void renormalizeLessonOrder(Long chapterId) {
        List<Lesson> list = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(chapterId);
        for (int i = 0; i < list.size(); i++) list.get(i).setOrderIndex(i);
    }
    private void renormalizeSectionOrder(Long lessonId) {
        List<Section> list = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lessonId);
        for (int i = 0; i < list.size(); i++) list.get(i).setOrderIndex(i);
    }
    private void renormalizeContentOrder(Long sectionId) {
        List<SectionsContent> list = contentRepo.findBySection_IdOrderByOrderIndexAsc(sectionId);
        for (int i = 0; i < list.size(); i++) list.get(i).setOrderIndex(i);
    }

}
