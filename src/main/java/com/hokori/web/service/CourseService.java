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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final UserRepository userRepo;

    // =========================
    // COURSE
    // =========================
    public CourseRes createCourse(Long teacherUserId, @Valid CourseUpsertReq r) {
        Course c = new Course();
        c.setUserId(teacherUserId);
        applyCourse(c, r);
        
        // Generate unique slug with retry logic to handle race conditions
        String title = r.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "Untitled Course";
        }
        c.setSlug(generateUniqueSlugWithRetry(title));
        
        // Save with retry if duplicate slug constraint violation
        int maxRetries = 5;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                c = courseRepo.save(c);
                break; // Success, exit retry loop
            } catch (DataIntegrityViolationException e) {
                if (e.getMessage() != null && e.getMessage().contains("slug") && e.getMessage().contains("unique")) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, 
                            "Unable to generate unique slug after " + maxRetries + " attempts. Please try again.");
                    }
                    // Generate new slug with timestamp to ensure uniqueness
                    c.setSlug(generateUniqueSlugWithTimestamp(title));
                } else {
                    throw e; // Re-throw if it's not a slug constraint violation
                }
            }
        }

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
        String oldSlug = c.getSlug();
        applyCourse(c, r);
        
        // Only update slug if title changed
        String newTitle = r.getTitle();
        if (newTitle == null || newTitle.trim().isEmpty()) {
            newTitle = "Untitled Course";
        }
        String newSlugBase = SlugUtil.toSlug(newTitle);
        if (!newSlugBase.equals(oldSlug)) {
            // Generate unique slug (excluding current course from check)
            c.setSlug(generateUniqueSlugForUpdate(newTitle, id));
            
            // Retry save if duplicate slug constraint violation
            int maxRetries = 5;
            int retryCount = 0;
            while (retryCount < maxRetries) {
                try {
                    c = courseRepo.save(c);
                    break; // Success
                } catch (DataIntegrityViolationException e) {
                    if (e.getMessage() != null && e.getMessage().contains("slug") && e.getMessage().contains("unique")) {
                        retryCount++;
                        if (retryCount >= maxRetries) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                                "Unable to generate unique slug after " + maxRetries + " attempts. Please try again.");
                        }
                        c.setSlug(generateUniqueSlugWithTimestamp(newTitle));
                    } else {
                        throw e;
                    }
                }
            }
        }
        return toCourseResLite(c);
    }

    public CourseRes updateCoverImage(Long courseId, Long teacherUserId, String coverImagePath) {
        Course c = getOwned(courseId, teacherUserId);  // đã check owner + deletedFlag
        c.setCoverImagePath(coverImagePath);
        return toCourseResLite(c);
    }

    public void softDelete(Long id, Long teacherUserId) {
        getOwned(id, teacherUserId).setDeletedFlag(true);
    }

    /**
     * Submit course for moderator approval (changed from direct publish)
     * 
     * NOTE: Currently only requires title. Content validation (chapters, lessons, sections) 
     * will be added later when content management is implemented.
     */
    public CourseRes submitForApproval(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);

        // Chỉ cho phép submit từ DRAFT
        if (c.getStatus() != CourseStatus.DRAFT) {
            throw bad("Course must be in DRAFT status to submit for approval");
        }

        // Validate title is not empty
        if (c.getTitle() == null || c.getTitle().trim().isEmpty()) {
            throw bad("Course title is required");
        }

        // Đúng 1 chapter học thử (tự động tạo khi tạo course)
        long trialCount = chapterRepo.countByCourse_IdAndIsTrialTrue(id);
        if (trialCount != 1) {
            throw bad("Course must have exactly ONE trial chapter");
        }

        // TODO: Validate cấu trúc sections khi có nội dung
        // Hiện tại chỉ cần title và trial chapter là đủ để submit
        // Validation sections sẽ được thêm sau khi có nội dung
        /*
        c.getChapters().forEach(ch ->
                ch.getLessons().forEach(ls ->
                        ls.getSections().forEach(this::validateSectionBeforePublish)
                )
        );
        */

        // Chuyển sang PENDING_APPROVAL thay vì PUBLISHED
        c.setStatus(CourseStatus.PENDING_APPROVAL);
        return toCourseResLite(c);
    }

    /**
     * Approve course by moderator (publish course)
     */
    public CourseRes approveCourse(Long id, Long moderatorUserId) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        if (c.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw bad("Course must be in PENDING_APPROVAL status to approve");
        }

        c.setStatus(CourseStatus.PUBLISHED);
        c.setPublishedAt(Instant.now());
        return toCourseResLite(c);
    }

    /**
     * Reject course by moderator (back to DRAFT)
     */
    public CourseRes rejectCourse(Long id, Long moderatorUserId, String reason) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        if (c.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw bad("Course must be in PENDING_APPROVAL status to reject");
        }

        c.setStatus(CourseStatus.DRAFT);
        // TODO: Có thể lưu rejection reason vào một field riêng nếu cần
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
        // Use native query to check existence and avoid LOB loading
        Object[] metadata = courseRepo.findCourseMetadataById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Map metadata to CourseRes (without description to avoid LOB)
        CourseRes courseRes = mapCourseMetadataToRes(metadata);
        Long courseId = courseRes.getId();

        var chapterEntities = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
        var chapterDtos = new ArrayList<ChapterRes>();

        for (var ch : chapterEntities) {
            var lessonEntities = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(ch.getId());
            var lessonDtos = new ArrayList<LessonRes>();

            for (var ls : lessonEntities) {
                var sectionEntities = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(ls.getId());
                var sectionDtos = new ArrayList<SectionRes>();

                for (var s : sectionEntities) {
                    var contentEntities = contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId());
                    var contentDtos = new ArrayList<ContentRes>(contentEntities.size());
                    for (var ct : contentEntities) {
                        contentDtos.add(new ContentRes(
                                ct.getId(),
                                ct.getOrderIndex(),
                                ct.getContentFormat(),
                                ct.isPrimaryContent(),
                                ct.getFilePath(),
                                ct.getRichText(),
                                ct.getFlashcardSetId()
                        ));
                    }
                    sectionDtos.add(new SectionRes(
                            s.getId(),
                            s.getTitle(),
                            s.getOrderIndex(),
                            s.getStudyType(),
                            s.getFlashcardSetId(),
                            contentDtos
                    ));
                }

                lessonDtos.add(new LessonRes(
                        ls.getId(),
                        ls.getTitle(),
                        ls.getOrderIndex(),
                        ls.getTotalDurationSec(),
                        sectionDtos
                ));
            }

            chapterDtos.add(new ChapterRes(
                    ch.getId(),
                    ch.getTitle(),
                    ch.getOrderIndex(),
                    ch.getSummary(),
                    lessonDtos
            ));
        }

        // Set chapters to courseRes and return
        courseRes.setChapters(chapterDtos);
        return courseRes;
    }

    @Transactional(readOnly = true)
    public CourseRes getTrialTree(Long courseId) {
        // Use native query to check existence and avoid LOB loading
        Object[] metadata = courseRepo.findCourseMetadataById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Map metadata to CourseRes (without description to avoid LOB)
        CourseRes courseRes = mapCourseMetadataToRes(metadata);
        
        Chapter trial = chapterRepo.findByCourse_IdAndIsTrialTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No trial chapter"));
        
        // Convert trial chapter to DTO
        List<ChapterRes> chapterDtos = List.of(toChapterResShallow(trial));
        courseRes.setChapters(chapterDtos);
        
        return courseRes;
    }

    @Transactional(readOnly = true)
    public Page<CourseRes> listMine(Long teacherUserId, int page, int size, String q, CourseStatus status) {
        // Use native query to avoid LOB stream errors
        String statusStr = status != null ? status.name() : null;
        String searchQ = (q != null && !q.isBlank()) ? q.trim() : null;

        List<Object[]> metadataList = courseRepo.findCourseMetadataByUserId(teacherUserId, statusStr, searchQ);

        // Manual pagination
        int total = metadataList.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        List<Object[]> pagedList = (start < total) ? metadataList.subList(start, end) : Collections.emptyList();

        // Map to CourseRes (description will be null to avoid LOB loading)
        List<CourseRes> content = pagedList.stream()
                .map(this::mapCourseMetadataToRes)
                .collect(Collectors.toList());

        return new PageImpl<>(content, PageRequest.of(page, size, Sort.by("updatedAt").descending()), total);
    }

    private CourseRes mapCourseMetadataToRes(Object[] metadata) {
        if (metadata == null || metadata.length == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid metadata array");
        }
        
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }
        
        // Validate array length (should have at least 13 elements now with teacherName)
        if (actualMetadata.length < 13) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Course metadata array too short: expected at least 13 elements, got " + actualMetadata.length);
        }

        // [id, title, slug, subtitle, level, priceCents, discountedPriceCents,
        //  currency, coverImagePath, status, publishedAt, userId, deletedFlag, teacherName]
        Long id = actualMetadata[0] != null ? ((Number) actualMetadata[0]).longValue() : null;
        String title = actualMetadata[1] != null ? actualMetadata[1].toString() : null;
        String slug = actualMetadata[2] != null ? actualMetadata[2].toString() : null;
        String subtitle = actualMetadata[3] != null ? actualMetadata[3].toString() : null;
        JLPTLevel level = actualMetadata[4] != null 
            ? JLPTLevel.valueOf(actualMetadata[4].toString().toUpperCase()) 
            : JLPTLevel.N5;
        Long priceCents = actualMetadata[5] != null ? ((Number) actualMetadata[5]).longValue() : null;
        Long discountedPriceCents = actualMetadata[6] != null ? ((Number) actualMetadata[6]).longValue() : null;
        String currency = actualMetadata[7] != null ? actualMetadata[7].toString() : "VND";
        String coverImagePath = actualMetadata[8] != null ? actualMetadata[8].toString() : null;
        CourseStatus courseStatus = actualMetadata[9] != null
            ? CourseStatus.valueOf(actualMetadata[9].toString().toUpperCase())
            : CourseStatus.DRAFT;
        Instant publishedAt = actualMetadata[10] != null
                ? (actualMetadata[10] instanceof Instant
                ? (Instant) actualMetadata[10]
                : Instant.ofEpochMilli(((java.sql.Timestamp) actualMetadata[10]).getTime()))
                : null;
        Long userId = actualMetadata[11] != null ? ((Number) actualMetadata[11]).longValue() : null;
        String teacherName = actualMetadata[13] != null ? actualMetadata[13].toString() : null;

        CourseRes res = new CourseRes();
        res.setId(id);
        res.setTitle(title);
        res.setSlug(slug);
        res.setSubtitle(subtitle);
        res.setDescription(null); // tránh LOB
        res.setLevel(level);
        res.setPriceCents(priceCents);
        res.setDiscountedPriceCents(discountedPriceCents);
        res.setCurrency(currency);
        res.setCoverImagePath(coverImagePath);
        res.setStatus(courseStatus);
        res.setPublishedAt(publishedAt);
        res.setUserId(userId);
        res.setTeacherName(teacherName);
        res.setChapters(Collections.emptyList());
        return res;
    }

    @Transactional(readOnly = true)
    public Page<CourseRes> listPublished(JLPTLevel level, int page, int size) {
        String levelStr = level != null ? level.name() : null;

        List<Object[]> metadataList = courseRepo.findPublishedCourseMetadata(levelStr);

        int total = metadataList.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        List<Object[]> pagedList = (start < total) ? metadataList.subList(start, end) : Collections.emptyList();

        List<CourseRes> content = pagedList.stream()
                .map(this::mapCourseMetadataToRes)
                .collect(Collectors.toList());

        return new PageImpl<>(content, PageRequest.of(page, size, Sort.by("publishedAt").descending()), total);
    }

    /**
     * List courses pending approval (for moderator)
     */
    @Transactional(readOnly = true)
    public List<CourseRes> listPendingApprovalCourses() {
        List<Object[]> metadataList = courseRepo.findPendingApprovalCourses();
        return metadataList.stream()
                .map(this::mapCourseMetadataToRes)
                .collect(Collectors.toList());
    }

    // =========================
    // CHILDREN
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
        ct.setFilePath(r.getFilePath());
        ct.setRichText(r.getRichText());
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

        // mặc định ASSET nếu null
        if (fmt == null || fmt == ContentFormat.ASSET) {
            // ASSET: require filePath
            if (r.getFilePath() == null || r.getFilePath().isBlank()) {
                throw bad("filePath is required for ASSET");
            }

            // GRAMMAR: chỉ 1 primaryContent (video chính)
            if (section.getStudyType() == ContentType.GRAMMAR && r.isPrimaryContent()) {
                long primaryCount = section.getContents().stream()
                        .filter(SectionsContent::isPrimaryContent)
                        .count();
                if (primaryCount >= 1) {
                    throw bad("GRAMMAR requires exactly ONE primary video (already set)");
                }
            }

        } else if (fmt == ContentFormat.RICH_TEXT) {
            // RICH_TEXT: phải có richText, không được primary
            if (r.getRichText() == null || r.getRichText().isBlank()) {
                throw bad("richText is required for RICH_TEXT");
            }
            if (r.isPrimaryContent()) {
                throw bad("primaryContent must be false for RICH_TEXT");
            }

        } else if (fmt == ContentFormat.FLASHCARD_SET) {
            // FLASHCARD_SET: chỉ cho VOCAB
            if (section.getStudyType() != ContentType.VOCABULARY) {
                throw bad("FLASHCARD_SET only allowed in VOCAB sections");
            }
            if (r.getFlashcardSetId() == null) {
                throw bad("flashcardSetId required for FLASHCARD_SET");
            }

        } else {
            // Không còn QUIZ_REF
            throw bad("Unsupported content format");
        }
    }

    private void validateSectionBeforePublish(Section s) {
        if (s.getStudyType() == ContentType.VOCABULARY) {
            if (s.getFlashcardSetId() == null) {
                throw bad("VOCAB section must link a flashcard set");
            }

        } else if (s.getStudyType() == ContentType.GRAMMAR) {
            long primary = s.getContents().stream()
                    .filter(SectionsContent::isPrimaryContent)
                    .count();
            if (primary != 1) {
                throw bad("GRAMMAR section requires exactly ONE primary video");
            }

        } else if (s.getStudyType() == ContentType.KANJI) {
            long primary = s.getContents().stream()
                    .filter(SectionsContent::isPrimaryContent)
                    .count();
            if (primary < 1) {
                throw bad("KANJI section requires at least ONE primary content (video or doc)");
            }
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
        c.setCoverImagePath(r.getCoverImagePath());
    }

    /**
     * Generate unique slug - checks all records (including deleted) for uniqueness
     * This method is kept for backward compatibility but uses the same logic as generateUniqueSlugWithRetry
     */
    private String uniqueSlug(String title) {
        return generateUniqueSlugWithRetry(title);
    }
    
    /**
     * Generate unique slug with retry logic to handle race conditions.
     * Checks all records (including deleted) because unique constraint applies to entire table.
     * Compatible with PostgreSQL on Railway.
     */
    private String generateUniqueSlugWithRetry(String title) {
        if (title == null || title.trim().isEmpty()) {
            title = "untitled-course";
        }
        String base = SlugUtil.toSlug(title);
        if (base.isEmpty()) {
            base = "untitled-course";
        }
        
        // First try base slug
        String s = base;
        if (!courseRepo.existsBySlug(s)) {
            return s;
        }
        
        // If exists, try with incremental number
        int i = 1;
        while (i <= 1000) {
            s = base + "-" + i;
            if (!courseRepo.existsBySlug(s)) {
                return s;
            }
            i++;
        }
        
        // Fallback: use timestamp to ensure uniqueness (handles race conditions)
        return generateUniqueSlugWithTimestamp(title);
    }
    
    /**
     * Generate unique slug for update operation - excludes current course from check.
     * This prevents false positives when updating a course with the same slug.
     * Checks all records (including deleted) because unique constraint applies to entire table.
     */
    private String generateUniqueSlugForUpdate(String title, Long excludeCourseId) {
        if (title == null || title.trim().isEmpty()) {
            title = "untitled-course";
        }
        String base = SlugUtil.toSlug(title);
        if (base.isEmpty()) {
            base = "untitled-course";
        }
        
        // First try base slug - check if exists AND not the current course
        String s = base;
        if (!courseRepo.existsBySlug(s)) {
            return s; // Slug doesn't exist, use it
        }
        // Slug exists, check if it's the current course
        Optional<Course> existing = courseRepo.findBySlugAndDeletedFlagFalse(s);
        if (existing.isPresent() && existing.get().getId().equals(excludeCourseId)) {
            return s; // It's the current course, keep the slug
        }
        
        // Slug exists and belongs to different course, try with incremental number
        int i = 1;
        while (i <= 1000) {
            s = base + "-" + i;
            if (!courseRepo.existsBySlug(s)) {
                return s; // Found available slug
            }
            // Check if it's the current course
            existing = courseRepo.findBySlugAndDeletedFlagFalse(s);
            if (existing.isPresent() && existing.get().getId().equals(excludeCourseId)) {
                return s; // It's the current course, keep the slug
            }
            i++;
        }
        
        // Fallback: use timestamp to ensure uniqueness
        return generateUniqueSlugWithTimestamp(title);
    }
    
    /**
     * Generate unique slug with timestamp to ensure uniqueness in race conditions.
     * Used as fallback when incremental numbering fails or during concurrent requests.
     * Compatible with PostgreSQL on Railway.
     */
    private String generateUniqueSlugWithTimestamp(String title) {
        if (title == null || title.trim().isEmpty()) {
            title = "untitled-course";
        }
        String base = SlugUtil.toSlug(title);
        if (base.isEmpty()) {
            base = "untitled-course";
        }
        
        // Use timestamp + random suffix for better uniqueness
        long timestamp = System.currentTimeMillis();
        int randomSuffix = (int) (Math.random() * 1000); // 0-999
        String suffix = timestamp + "-" + randomSuffix;
        
        // Ensure total length doesn't exceed 180 chars (slug column limit)
        int maxBaseLength = 180 - suffix.length() - 1; // -1 for dash
        if (maxBaseLength < 1) {
            maxBaseLength = 1;
        }
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
        }
        
        String s = base + "-" + suffix;
        
        // Double-check uniqueness (very unlikely but possible)
        int retry = 0;
        while (courseRepo.existsBySlug(s) && retry < 10) {
            randomSuffix = (int) (Math.random() * 10000);
            suffix = timestamp + "-" + randomSuffix;
            maxBaseLength = 180 - suffix.length() - 1;
            if (maxBaseLength < 1) maxBaseLength = 1;
            if (base.length() > maxBaseLength) {
                base = base.substring(0, maxBaseLength);
            }
            s = base + "-" + suffix;
            retry++;
        }
        
        return s;
    }

    private Course getOwned(Long id, Long teacherUserId) {
        // Use native query to check ownership without loading LOB fields
        Object[] metadata = courseRepo.findCourseMetadataById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Handle nested array case (PostgreSQL)
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }
        
        // Validate array length
        if (actualMetadata.length < 12 || actualMetadata[11] == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Invalid course metadata: missing userId");
        }
        
        Long userId = ((Number) actualMetadata[11]).longValue(); // userId is at index 11
        if (!Objects.equals(userId, teacherUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
        }
        
        // Use getReferenceById to avoid loading full entity with LOB
        return courseRepo.getReferenceById(id);
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

    /**
     * Helper method to get teacher name (displayName or username) from userId
     */
    private String getTeacherName(Long userId) {
        if (userId == null) {
            return null;
        }
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        User u = userOpt.get();
        return (u.getDisplayName() != null && !u.getDisplayName().isEmpty()) 
            ? u.getDisplayName() 
            : u.getUsername();
    }

    private CourseRes toCourseResLite(Course c) {
        CourseRes res = new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(),
                c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(),
                c.getCoverImagePath(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                null, // teacherName - will set below
                List.of()
        );
        res.setTeacherName(getTeacherName(c.getUserId()));
        return res;
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

        CourseRes res = new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(),
                c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(),
                c.getCoverImagePath(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                null, // teacherName - will set below
                chapters
        );
        res.setTeacherName(getTeacherName(c.getUserId()));
        return res;
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

        CourseRes res = new CourseRes(
                c.getId(), c.getTitle(), c.getSlug(), c.getSubtitle(),
                c.getDescription(), c.getLevel(),
                c.getPriceCents(), c.getDiscountedPriceCents(), c.getCurrency(),
                c.getCoverImagePath(),
                c.getStatus(), c.getPublishedAt(), c.getUserId(),
                null, // teacherName - will set below
                chapters
        );
        res.setTeacherName(getTeacherName(c.getUserId()));
        return res;
    }

    private ChapterRes toChapterResShallow(Chapter ch) {
        return new ChapterRes(ch.getId(), ch.getTitle(), ch.getOrderIndex(), ch.getSummary(), List.of());
    }

    private LessonRes toLessonResShallow(Lesson ls) {
        return new LessonRes(ls.getId(), ls.getTitle(), ls.getOrderIndex(), ls.getTotalDurationSec(), List.of());
    }

    private SectionRes toSectionResShallow(Section s) {
        return new SectionRes(
                s.getId(),
                s.getTitle(),
                s.getOrderIndex(),
                s.getStudyType(),
                s.getFlashcardSetId(),
                List.of()
        );
    }

    private SectionRes toSectionResFull(Section s) {
        List<ContentRes> contents = s.getContents().stream()
                .sorted(Comparator.comparing(SectionsContent::getOrderIndex))
                .map(this::toContentRes)
                .collect(Collectors.toList());
        return new SectionRes(
                s.getId(),
                s.getTitle(),
                s.getOrderIndex(),
                s.getStudyType(),
                s.getFlashcardSetId(),
                contents
        );
    }

    private LessonRes toLessonResFull(Lesson ls) {
        List<SectionRes> sections = ls.getSections().stream()
                .sorted(Comparator.comparing(Section::getOrderIndex))
                .map(this::toSectionResFull)
                .collect(Collectors.toList());
        return new LessonRes(
                ls.getId(),
                ls.getTitle(),
                ls.getOrderIndex(),
                ls.getTotalDurationSec(),
                sections
        );
    }

    private ContentRes toContentRes(SectionsContent c) {
        return new ContentRes(
                c.getId(),
                c.getOrderIndex(),
                c.getContentFormat(),
                c.isPrimaryContent(),
                c.getFilePath(),
                c.getRichText(),
                c.getFlashcardSetId()
        );
    }

    // =========================
    // COURSE DETAIL (metadata only)
    // =========================
    @Transactional(readOnly = true)
    public CourseRes getDetail(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);
        return toCourseResLite(c);
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

        if (r.getContentFormat() != null) {
            c.setContentFormat(r.getContentFormat());
        }
        c.setPrimaryContent(r.isPrimaryContent());
        c.setFilePath(r.getFilePath());
        c.setRichText(r.getRichText());
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

        int max = ordered.size() - 1;
        int idx = Math.max(0, Math.min(newIndex, max));

        T target = null;
        Iterator<T> it = ordered.iterator();
        while (it.hasNext()) {
            T cur = it.next();
            Long id = extractId(cur);
            if (Objects.equals(id, targetId)) {
                target = cur;
                it.remove();
                break;
            }
        }
        if (target == null) throw bad("Target not in list");

        ordered.add(idx, target);
        for (int i = 0; i < ordered.size(); i++) {
            setIndex.accept(ordered.get(i), i);
        }
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

    @Transactional(readOnly = true)
    public CourseRes getPublishedTree(Long courseId) {
        // Use native query to check status without loading LOB fields
        Object[] metadata = courseRepo.findCourseMetadataById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Handle nested array case (PostgreSQL)
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }
        
        // Validate array length
        if (actualMetadata.length < 10 || actualMetadata[9] == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Invalid course metadata: missing status");
        }
        
        // Check status (at index 9)
        CourseStatus status;
        try {
            status = CourseStatus.valueOf(actualMetadata[9].toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Invalid course status: " + actualMetadata[9]);
        }
        if (status != CourseStatus.PUBLISHED) {
            // Ẩn sự tồn tại nếu chưa publish
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course is not published");
        }
        return getTree(courseId);
    }

    /**
     * Get full tree of a course pending approval (for moderator review)
     * Only allows access to courses with PENDING_APPROVAL status
     */
    @Transactional(readOnly = true)
    public CourseRes getPendingApprovalTree(Long courseId) {
        // Use native query to check status without loading LOB fields
        Object[] metadata = courseRepo.findCourseMetadataById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Handle nested array case (PostgreSQL)
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }
        
        // Validate array length
        if (actualMetadata.length < 10 || actualMetadata[9] == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Invalid course metadata: missing status");
        }
        
        // Check status (at index 9)
        CourseStatus status;
        try {
            status = CourseStatus.valueOf(actualMetadata[9].toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Invalid course status: " + actualMetadata[9]);
        }
        if (status != CourseStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Course is not pending approval. Current status: " + status);
        }
        return getTree(courseId);
    }
}
