package com.hokori.web.service;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.Enum.ContentFormat;
import com.hokori.web.Enum.ContentType;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.dto.course.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import com.hokori.web.util.SlugUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final EnrollmentRepository enrollmentRepo;
    private final com.hokori.web.service.NotificationService notificationService;
    private final QuizRepository quizRepo;
    private final FlashcardSetRepository flashcardSetRepo;
    private final QuestionRepository questionRepo;
    private final OptionRepository optionRepo;
    private final FileStorageService fileStorageService;
    private final com.hokori.web.service.CourseFlagService courseFlagService;
    private final ObjectMapper objectMapper;

    // =========================
    // COURSE
    // =========================
    public CourseRes createCourse(Long teacherUserId, @Valid CourseUpsertReq r) {
        Course c = new Course();
        c.setUserId(teacherUserId);
        // Ensure snapshotData is null (not included in INSERT due to insertable=false and @DynamicInsert)
        c.setSnapshotData(null);
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
        
        // Lưu giá trị cũ để so sánh
        Long oldPriceCents = c.getPriceCents();
        Long oldDiscountedPriceCents = c.getDiscountedPriceCents();
        CourseStatus oldStatus = c.getStatus();
        
        applyCourse(c, r);

        // Only update slug if title changed
        String newTitle = r.getTitle();
        if (newTitle == null || newTitle.trim().isEmpty()) {
            newTitle = "Untitled Course";
        }
        String newSlugBase = SlugUtil.toSlug(newTitle);
        boolean titleChanged = !newSlugBase.equals(oldSlug);
        if (titleChanged) {
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
        
        // BR-03: Nếu course đang PUBLISHED và có thay đổi thông tin cốt lõi (title, price)
        // thì tự động chuyển về PENDING_APPROVAL để moderator review lại
        boolean priceChanged = !Objects.equals(oldPriceCents, c.getPriceCents()) 
                || !Objects.equals(oldDiscountedPriceCents, c.getDiscountedPriceCents());
        
        if (oldStatus == CourseStatus.PUBLISHED && (titleChanged || priceChanged)) {
            autoSubmitForApprovalIfPublished(c, teacherUserId, 
                titleChanged ? "title" : null, 
                priceChanged ? "price" : null);
        }
        
        return toCourseResLite(c);
    }

    public CourseRes updateCoverImage(Long courseId, Long teacherUserId, String coverImagePath) {
        Course c = getOwned(courseId, teacherUserId);  // đã check owner + deletedFlag
        c.setCoverImagePath(coverImagePath);
        courseRepo.save(c); // Save to persist changes
        
        // Use native query to avoid loading description LOB field
        Object[] metadata = courseRepo.findCourseMetadataById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return mapCourseMetadataToRes(metadata);
    }

    public void softDelete(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);
        
        // Only allow deleting courses in DRAFT or REJECTED status
        // Cannot delete PUBLISHED, PENDING_APPROVAL, PENDING_UPDATE, FLAGGED, or ARCHIVED courses
        CourseStatus status = c.getStatus();
        if (status != CourseStatus.DRAFT && status != CourseStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Cannot delete course with status " + status + ". Only DRAFT or REJECTED courses can be deleted.");
        }
        
        c.setDeletedFlag(true);
        courseRepo.save(c);
    }

    /**
     * BR-03: Tự động submit course về PENDING_APPROVAL nếu course đang PUBLISHED
     * và có thay đổi thông tin cốt lõi (title, price, syllabus structure, trial chapter)
     * 
     * @param course Course entity đã được update
     * @param teacherUserId Teacher user ID
     * @param changedFields Mô tả các field đã thay đổi (để log/notification)
     */
    private void autoSubmitForApprovalIfPublished(Course course, Long teacherUserId, String... changedFields) {
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            return; // Chỉ áp dụng cho course đang PUBLISHED
        }
        
        // Check teacher approval status - must be APPROVED
        User teacher = userRepo.findById(teacherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        
        if (teacher.getApprovalStatus() != ApprovalStatus.APPROVED) {
            // Không throw error, chỉ log warning vì đây là auto-submit
            // Teacher sẽ phải submit manually sau khi được approve
            return;
        }
        
        // Validate title is not empty
        if (course.getTitle() == null || course.getTitle().trim().isEmpty()) {
            return; // Không auto-submit nếu title rỗng
        }
        
        // Validate có đúng 1 trial chapter
        long trialCount = chapterRepo.countByCourse_IdAndIsTrialTrue(course.getId());
        if (trialCount != 1) {
            return; // Không auto-submit nếu không có đúng 1 trial chapter
        }
        
        // Chuyển sang PENDING_APPROVAL
        course.setStatus(CourseStatus.PENDING_APPROVAL);
        // Clear rejection info khi auto-submit
        course.setRejectionReason(null);
        course.setRejectedAt(null);
        course.setRejectedByUserId(null);
        
        // Clear flag info khi auto-submit (course đã được update)
        course.setFlaggedReason(null);
        course.setFlaggedAt(null);
        course.setFlaggedByUserId(null);
        
        courseRepo.save(course);
        
        // Tạo notification cho teacher
        String fieldsDesc = changedFields != null && changedFields.length > 0 
                ? String.join(", ", Arrays.stream(changedFields).filter(Objects::nonNull).toArray(String[]::new))
                : "core information";
        notificationService.notifyCourseSubmitted(course.getUserId(), course.getId(), 
            course.getTitle() + " (auto-submitted due to changes in: " + fieldsDesc + ")");
    }
    
    /**
     * Submit course for moderator approval (or resubmit after rejection)
     * 
     * Can submit from status: DRAFT, REJECTED, or FLAGGED
     * When resubmitting from REJECTED, rejection info will be cleared.
     *
     * NOTE: Currently only requires title. Content validation (chapters, lessons, sections)
     * will be added later when content management is implemented.
     * 
     * REQUIREMENT: Teacher must be APPROVED before submitting course for approval.
     */
    public CourseRes submitForApproval(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);

        // Check teacher approval status - must be APPROVED to submit course
        User teacher = userRepo.findById(teacherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        
        if (teacher.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Teacher profile must be approved before submitting courses. Current status: " + 
                    (teacher.getApprovalStatus() != null ? teacher.getApprovalStatus() : "NONE") + 
                    ". Please submit your teacher approval request first.");
        }

        if (!(c.getStatus() == CourseStatus.DRAFT
                || c.getStatus() == CourseStatus.REJECTED
                || c.getStatus() == CourseStatus.FLAGGED)) {
            throw bad("Course must be in DRAFT / REJECTED / FLAGGED to submit for approval");
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
        // Clear rejection info khi submit lại
        c.setRejectionReason(null);
        c.setRejectedAt(null);
        c.setRejectedByUserId(null);
        
        // Tạo notification cho teacher
        notificationService.notifyCourseSubmitted(c.getUserId(), c.getId(), c.getTitle());
        
        return toCourseResLite(c);
    }

    /**
     * Submit update for published course.
     * Course remains PUBLISHED with old content visible to learners.
     * Moderator can review and approve/reject the update.
     * 
     * Can only submit update from PUBLISHED status.
     * Sets status to PENDING_UPDATE and pendingUpdateAt timestamp.
     * 
     * REQUIREMENT: Teacher must be APPROVED before submitting updates.
     */
    public CourseRes submitUpdate(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);

        // Check teacher approval status - must be APPROVED to submit update
        User teacher = userRepo.findById(teacherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        
        if (teacher.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Teacher profile must be approved before submitting course updates. Current status: " + 
                    (teacher.getApprovalStatus() != null ? teacher.getApprovalStatus() : "NONE") + 
                    ". Please submit your teacher approval request first.");
        }

        if (c.getStatus() != CourseStatus.PUBLISHED) {
            throw bad("Course must be in PUBLISHED status to submit update");
        }

        // Validate title is not empty
        if (c.getTitle() == null || c.getTitle().trim().isEmpty()) {
            throw bad("Course title is required");
        }

        // Đúng 1 chapter học thử
        long trialCount = chapterRepo.countByCourse_IdAndIsTrialTrue(id);
        if (trialCount != 1) {
            throw bad("Course must have exactly ONE trial chapter");
        }

        // Create snapshot of current course tree (old content) before setting PENDING_UPDATE
        CourseRes currentCourseTree = getTree(id);
        String snapshotJson = createSnapshot(currentCourseTree);
        
        // Set status to PENDING_UPDATE and record timestamp
        c.setStatus(CourseStatus.PENDING_UPDATE);
        c.setPendingUpdateAt(Instant.now());
        // Use native query to update snapshot_data with JSONB cast for PostgreSQL compatibility
        if (snapshotJson != null && !snapshotJson.trim().isEmpty()) {
            courseRepo.updateSnapshotData(id, snapshotJson);
        } else {
            courseRepo.updateSnapshotData(id, null);
        }
        courseRepo.save(c); // Save other fields
        
        // Clear rejection info if any
        c.setRejectionReason(null);
        c.setRejectedAt(null);
        c.setRejectedByUserId(null);
        
        // Clear flag info if any
        c.setFlaggedReason(null);
        c.setFlaggedAt(null);
        c.setFlaggedByUserId(null);
        
        courseRepo.save(c);
        
        // Tạo notification cho teacher
        notificationService.notifyCourseSubmitted(c.getUserId(), c.getId(), 
            c.getTitle() + " (update submitted)");
        
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
        // Clear rejection info khi approve
        c.setRejectionReason(null);
        c.setRejectedAt(null);
        c.setRejectedByUserId(null);
        
        // Clear flag info khi approve (course đã được review lại và approve)
        c.setFlaggedReason(null);
        c.setFlaggedAt(null);
        c.setFlaggedByUserId(null);
        
        // Xóa tất cả CourseFlag records (flags từ users) khi approve lại
        // Vì course đã được review và approve lại, không cần giữ flags cũ
        courseFlagService.clearCourseFlags(id);
        
        // Tạo notification cho teacher
        notificationService.notifyCourseApproved(c.getUserId(), c.getId(), c.getTitle());
        
        return toCourseResLite(c);
    }

    /**
     * Reject course by moderator: PENDING_APPROVAL -> REJECTED
     * Teacher can edit and submit again after rejection.
     * 
     * @param id Course ID
     * @param moderatorUserId Moderator user ID who rejects
     * @param reason Rejection reason (optional, will be persisted)
     * @return Course response with REJECTED status and rejection info
     */
    public CourseRes rejectCourse(Long id, Long moderatorUserId, String reason) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (c.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw bad("Course must be in PENDING_APPROVAL status to reject");
        }

        c.setStatus(CourseStatus.REJECTED);
        c.setRejectionReason(reason);
        c.setRejectedAt(Instant.now());
        c.setRejectedByUserId(moderatorUserId);
        
        // Tạo notification cho teacher
        notificationService.notifyCourseRejected(c.getUserId(), c.getId(), c.getTitle(), reason);
        
        // toCourseResLite sẽ tự động map rejection info khi status = REJECTED
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
        
        // Load description separately to avoid LOB issues
        String description = courseRepo.findDescriptionById(courseId).orElse(null);
        courseRes.setDescription(description);
        
        // Note: Rejection info đã được map trong mapCourseMetadataToRes() khi status = REJECTED

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
                        // Filter out content with deleted quizId or flashcardSetId
                        if (ct.getQuizId() != null) {
                            Optional<Quiz> quizOpt = quizRepo.findById(ct.getQuizId());
                            if (quizOpt.isEmpty() || Boolean.TRUE.equals(quizOpt.get().getDeletedFlag())) {
                                continue; // Skip content with deleted quiz
                            }
                        }
                        if (ct.getFlashcardSetId() != null) {
                            Optional<FlashcardSet> setOpt = flashcardSetRepo.findById(ct.getFlashcardSetId());
                            if (setOpt.isEmpty() || setOpt.get().isDeletedFlag()) {
                                continue; // Skip content with deleted flashcard set
                            }
                        }
                        contentDtos.add(new ContentRes(
                                ct.getId(),
                                ct.getOrderIndex(),
                                ct.getContentFormat(),
                                ct.isPrimaryContent(),
                                ct.getFilePath(),
                                ct.getRichText(),
                                ct.getFlashcardSetId(),
                                ct.getQuizId()
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
                    ch.isTrial(),
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
        
        // Load description separately to avoid LOB issues
        String description = courseRepo.findDescriptionById(courseId).orElse(null);
        courseRes.setDescription(description);

        Chapter trial = chapterRepo.findByCourse_IdAndIsTrialTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No trial chapter"));

        // Load full trial chapter with lessons, sections, and contents (similar to getTree)
        var lessonEntities = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(trial.getId());
        var lessonDtos = new ArrayList<LessonRes>();

        for (var ls : lessonEntities) {
            var sectionEntities = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(ls.getId());
            var sectionDtos = new ArrayList<SectionRes>();

            for (var s : sectionEntities) {
                var contentEntities = contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId());
                var contentDtos = new ArrayList<ContentRes>(contentEntities.size());
                for (var ct : contentEntities) {
                    // Filter out content with deleted quizId or flashcardSetId
                    if (ct.getQuizId() != null) {
                        Optional<Quiz> quizOpt = quizRepo.findById(ct.getQuizId());
                        if (quizOpt.isEmpty() || Boolean.TRUE.equals(quizOpt.get().getDeletedFlag())) {
                            continue; // Skip content with deleted quiz
                        }
                    }
                    if (ct.getFlashcardSetId() != null) {
                        Optional<FlashcardSet> setOpt = flashcardSetRepo.findById(ct.getFlashcardSetId());
                        if (setOpt.isEmpty() || setOpt.get().isDeletedFlag()) {
                            continue; // Skip content with deleted flashcard set
                        }
                    }
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

        ChapterRes chapterRes = new ChapterRes(
                trial.getId(),
                trial.getTitle(),
                trial.getOrderIndex(),
                trial.getSummary(),
                trial.isTrial(),
                lessonDtos
        );

        courseRes.setChapters(List.of(chapterRes));

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

        // Validate array length (should have at least 14 elements: basic fields + teacherName)
        // Can have 17 elements if includes rejection fields (rejectionReason, rejectedAt, rejectedByUserId)
        // Can have 20 elements if includes flag fields (flaggedReason, flaggedAt, flaggedByUserId)
        // Can have 21 elements if includes pendingUpdateAt
        if (actualMetadata.length < 14) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Course metadata array too short: expected at least 14 elements, got " + actualMetadata.length);
        }
        
        boolean hasRejectionFields = actualMetadata.length >= 17;
        boolean hasFlagFields = actualMetadata.length >= 20;
        boolean hasPendingUpdateAt = actualMetadata.length >= 21;

        // [id, title, slug, subtitle, level, priceCents, discountedPriceCents,
        //  currency, coverImagePath, status, publishedAt, userId, deletedFlag, teacherName,
        //  rejectionReason, rejectedAt, rejectedByUserId]
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
        
        // Rejection fields (only if metadata includes them)
        String rejectionReason = null;
        Instant rejectedAt = null;
        Long rejectedByUserId = null;
        
        if (hasRejectionFields && actualMetadata.length >= 17) {
            rejectionReason = actualMetadata[14] != null ? actualMetadata[14].toString() : null;
            rejectedAt = actualMetadata[15] != null
                    ? (actualMetadata[15] instanceof Instant
                    ? (Instant) actualMetadata[15]
                    : actualMetadata[15] instanceof java.sql.Timestamp
                    ? Instant.ofEpochMilli(((java.sql.Timestamp) actualMetadata[15]).getTime())
                    : null)
                    : null;
            rejectedByUserId = actualMetadata[16] != null ? ((Number) actualMetadata[16]).longValue() : null;
        }
        
        // Flag fields (only if metadata includes them)
        String flaggedReason = null;
        Instant flaggedAt = null;
        Long flaggedByUserId = null;
        
        if (hasFlagFields && actualMetadata.length >= 20) {
            flaggedReason = actualMetadata[17] != null ? actualMetadata[17].toString() : null;
            flaggedAt = actualMetadata[18] != null
                    ? (actualMetadata[18] instanceof Instant
                    ? (Instant) actualMetadata[18]
                    : actualMetadata[18] instanceof java.sql.Timestamp
                    ? Instant.ofEpochMilli(((java.sql.Timestamp) actualMetadata[18]).getTime())
                    : null)
                    : null;
            flaggedByUserId = actualMetadata[19] != null ? ((Number) actualMetadata[19]).longValue() : null;
        }
        
        // Pending update field (only if metadata includes it)
        Instant pendingUpdateAt = null;
        if (hasPendingUpdateAt && actualMetadata.length >= 21) {
            pendingUpdateAt = actualMetadata[20] != null
                    ? (actualMetadata[20] instanceof Instant
                    ? (Instant) actualMetadata[20]
                    : actualMetadata[20] instanceof java.sql.Timestamp
                    ? Instant.ofEpochMilli(((java.sql.Timestamp) actualMetadata[20]).getTime())
                    : null)
                    : null;
        }

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

        // ✅ Quan trọng: set enrollCount ở đây
        long enrollCount = (id != null) ? enrollmentRepo.countByCourseId(id) : 0L;
        res.setEnrollCount(enrollCount);
        
        // Map rejection info (chỉ có khi status = REJECTED)
        if (courseStatus == CourseStatus.REJECTED) {
            res.setRejectionReason(rejectionReason);
            res.setRejectedAt(rejectedAt);
            res.setRejectedByUserId(rejectedByUserId);
            if (rejectedByUserId != null) {
                res.setRejectedByUserName(getTeacherName(rejectedByUserId));
            }
        }
        
        // Map flag info (chỉ có khi status = FLAGGED)
        if (courseStatus == CourseStatus.FLAGGED) {
            res.setFlaggedReason(flaggedReason);
            res.setFlaggedAt(flaggedAt);
            res.setFlaggedByUserId(flaggedByUserId);
            if (flaggedByUserId != null) {
                res.setFlaggedByUserName(getTeacherName(flaggedByUserId));
            }
            // Message thân thiện cho learner (không gây tiêu cực) - chỉ hiển thị cho enrolled learners
            res.setStatusMessage("Khóa học đang được cập nhật. Bạn vẫn có thể học tập bình thường.");
        }
        
        // Set canFlag cho moderator: chỉ có thể flag course có status = PUBLISHED
        res.setCanFlag(courseStatus == CourseStatus.PUBLISHED);
        
        // Set isModeratorFlagged: true nếu course đã được moderator flag (đã gửi thông báo cho teacher)
        res.setIsModeratorFlagged(courseStatus == CourseStatus.FLAGGED && flaggedByUserId != null);
        
        // Map pending update info (chỉ có khi status = PENDING_UPDATE)
        if (courseStatus == CourseStatus.PENDING_UPDATE) {
            res.setPendingUpdateAt(pendingUpdateAt);
        }

        res.setChapters(Collections.emptyList());
        return res;
    }

    @Transactional(readOnly = true)
    public Page<CourseRes> listPublished(JLPTLevel level, int page, int size) {
        return listPublished(level, page, size, null);
    }

    /**
     * List published courses with optional enrollment check for authenticated user
     * @param level JLPT level filter (optional)
     * @param page Page number (0-based)
     * @param size Page size
     * @param userId User ID to check enrollment (optional, null if not authenticated)
     * @return Page of CourseRes with isEnrolled field set
     */
    @Transactional(readOnly = true)
    public Page<CourseRes> listPublished(JLPTLevel level, int page, int size, Long userId) {
        String levelStr = level != null ? level.name() : null;

        List<Object[]> metadataList = courseRepo.findPublishedCourseMetadata(levelStr);

        int total = metadataList.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        List<Object[]> pagedList = (start < total) ? metadataList.subList(start, end) : Collections.emptyList();

        // Get enrolled course IDs for this user (if authenticated)
        Set<Long> enrolledCourseIds = userId != null
                ? enrollmentRepo.findByUserId(userId).stream()
                        .map(e -> e.getCourseId())
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        final Set<Long> finalEnrolledCourseIds = enrolledCourseIds;
        List<CourseRes> content = pagedList.stream()
                .map(metadata -> {
                    CourseRes res = mapCourseMetadataToRes(metadata);
                    // Set isEnrolled field
                    if (userId != null && res.getId() != null) {
                        res.setIsEnrolled(finalEnrolledCourseIds.contains(res.getId()));
                    } else {
                        res.setIsEnrolled(null); // Not authenticated
                    }
                    return res;
                })
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

    /**
     * List courses pending update (for moderator)
     * Courses that are PENDING_UPDATE status (submitted update from PUBLISHED)
     */
    @Transactional(readOnly = true)
    public List<CourseRes> listPendingUpdateCourses() {
        List<Object[]> metadataList = courseRepo.findPendingUpdateCourses();
        return metadataList.stream()
                .map(this::mapCourseMetadataToRes)
                .collect(Collectors.toList());
    }

    /**
     * Approve course update by moderator.
     * Applies the update and changes status from PENDING_UPDATE back to PUBLISHED.
     */
    public CourseRes approveUpdate(Long id, Long moderatorUserId) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (c.getStatus() != CourseStatus.PENDING_UPDATE) {
            throw bad("Course must be in PENDING_UPDATE status to approve update");
        }

        // Apply update: change status back to PUBLISHED and clear pendingUpdateAt
        c.setStatus(CourseStatus.PUBLISHED);
        c.setPendingUpdateAt(null);
        // Clear snapshot after approval using native query for JSONB compatibility
        courseRepo.updateSnapshotData(id, null);
        courseRepo.save(c); // Save other fields
        
        // Clear any rejection/flag info if any
        c.setRejectionReason(null);
        c.setRejectedAt(null);
        c.setRejectedByUserId(null);
        c.setFlaggedReason(null);
        c.setFlaggedAt(null);
        c.setFlaggedByUserId(null);
        
        courseRepo.save(c);
        
        // Tạo notification cho teacher
        notificationService.notifyCourseApproved(c.getUserId(), c.getId(), 
            c.getTitle() + " (update approved)");
        
        return toCourseResLite(c);
    }

    /**
     * Reject course update by moderator.
     * Reverts course back to PUBLISHED status and clears pendingUpdateAt.
     * Teacher can edit and submit update again after rejection.
     * 
     * @param id Course ID
     * @param moderatorUserId Moderator user ID who rejects
     * @param reason Rejection reason (optional, will be persisted)
     * @return Course response with PUBLISHED status
     */
    public CourseRes rejectUpdate(Long id, Long moderatorUserId, String reason) {
        Course c = courseRepo.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (c.getStatus() != CourseStatus.PENDING_UPDATE) {
            throw bad("Course must be in PENDING_UPDATE status to reject update");
        }

        // Restore course tree from snapshot (revert to old content)
        if (c.getSnapshotData() != null && !c.getSnapshotData().trim().isEmpty()) {
            restoreCourseEntitiesFromSnapshot(c, c.getSnapshotData());
        }
        
        // Revert to PUBLISHED status and clear pendingUpdateAt
        c.setStatus(CourseStatus.PUBLISHED);
        c.setPendingUpdateAt(null);
        // Clear snapshot after restore using native query for JSONB compatibility
        courseRepo.updateSnapshotData(id, null);
        courseRepo.save(c); // Save other fields
        
        // Store rejection reason for teacher reference
        c.setRejectionReason(reason);
        c.setRejectedAt(Instant.now());
        c.setRejectedByUserId(moderatorUserId);
        
        // Clear flag info if any
        c.setFlaggedReason(null);
        c.setFlaggedAt(null);
        c.setFlaggedByUserId(null);
        
        courseRepo.save(c);
        
        // Tạo notification cho teacher
        notificationService.notifyCourseRejected(c.getUserId(), c.getId(), c.getTitle(), reason);
        
        return toCourseResLite(c);
    }

    // =========================
    // CHILDREN
    // =========================

    public ChapterRes createChapter(Long courseId, Long teacherUserId, ChapterUpsertReq r) {
        assertOwner(courseId, teacherUserId);

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Chapter ch = new Chapter();
        ch.setCourse(course);
        ch.setTitle(r.getTitle());
        ch.setSummary(r.getSummary());

        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(chapterRepo.countByCourse_Id(courseId))
                : r.getOrderIndex();
        ch.setOrderIndex(oi);

        // Chapter đầu tiên (orderIndex = 0) luôn phải là trial
        boolean trialChanged = false;
        if (oi == 0) {
            // Nếu đã có trial chapter khác, bỏ trial của nó
            chapterRepo.findByCourse_IdAndIsTrialTrue(courseId).ifPresent(old -> {
                old.setTrial(false);
            });
            ch.setTrial(true);
            trialChanged = true; // Có thể đã thay đổi trial chapter
        } else if (Boolean.TRUE.equals(r.getIsTrial())) {
            // Nếu teacher muốn set trial cho chapter khác, kiểm tra đã có trial chưa
            if (chapterRepo.countByCourse_IdAndIsTrialTrue(courseId) > 0) {
                throw bad("Course already has a trial chapter. The first chapter (orderIndex=0) is always the trial chapter.");
            }
            ch.setTrial(true);
            trialChanged = true; // Có thể đã thay đổi trial chapter
        }

        Chapter saved = chapterRepo.save(ch);
        
        // Đảm bảo chapter đầu tiên luôn là trial (sau khi save để có ID)
        ensureFirstChapterIsTrial(courseId);
        
        // BR-03: Nếu course đang PUBLISHED và thêm chapter (thay đổi syllabus structure) → auto-submit
        // Hoặc nếu trial chapter thay đổi → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            if (trialChanged) {
                autoSubmitForApprovalIfPublished(course, teacherUserId, "trial chapter");
            } else {
                autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (chapter added)");
            }
        }
        
        return toChapterResShallow(saved);
    }

    public LessonRes createLesson(Long chapterId, Long teacherUserId, LessonUpsertReq r) {
        Long courseId = chapterRepo.findCourseIdByChapterId(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));

        assertOwner(courseId, teacherUserId);
        
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Lesson ls = new Lesson();
        ls.setChapter(chapterRepo.getReferenceById(chapterId));
        ls.setTitle(r.getTitle());

        int oi = (r.getOrderIndex() == null)
                ? Math.toIntExact(lessonRepo.countByChapter_Id(chapterId))
                : r.getOrderIndex();
        ls.setOrderIndex(oi);
        ls.setTotalDurationSec(r.getTotalDurationSec() == null ? 0L : r.getTotalDurationSec());

        Lesson saved = lessonRepo.save(ls);
        
        // BR-03: Nếu course đang PUBLISHED và thêm lesson (thay đổi syllabus structure) → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (lesson added)");
        }
        
        return toLessonResShallow(saved);
    }

    public SectionRes createSection(Long lessonId, Long teacherUserId, SectionUpsertReq r) {
        Long courseId = lessonRepo.findCourseIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        assertOwner(courseId, teacherUserId);
        
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

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
        
        // BR-03: Nếu course đang PUBLISHED và thêm section (thay đổi syllabus structure) → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (section added)");
        }
        
        return toSectionResShallow(saved);
    }

    public ContentRes createContent(Long sectionId, Long teacherUserId, ContentUpsertReq r) {
        Long courseId = sectionRepo.findCourseIdBySectionId(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        assertOwner(courseId, teacherUserId);

        Section scRef = sectionRepo.getReferenceById(sectionId);
        validateContentPayload(scRef, r, null); // null = CREATE, không exclude content nào

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
        ct.setQuizId(r.getQuizId());

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
//        if (s.getStudyType() == ContentType.VOCABULARY && s.getFlashcardSetId() == null) {
//            throw bad("VOCABULARY section requires flashcardSetId");
//        }
    }

    /**
     * Validate content payload
     * @param section Section chứa content
     * @param r Request payload
     * @param excludeContentId Content ID cần loại trừ khỏi validation (null khi CREATE, không null khi UPDATE)
     */
    private void validateContentPayload(Section section, ContentUpsertReq r, Long excludeContentId) {
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
                        .filter(sc -> excludeContentId == null || !sc.getId().equals(excludeContentId)) // Loại trừ content đang update
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
            // ❌ KHÔNG bắt buộc flashcardSetId nữa
            // cho phép tạo content FLASHCARD_SET "placeholder" với flashcardSetId = null
            // flashcardSetId sẽ được gán sau khi tạo flashcard set ở module flashcards.

        } else if (fmt == ContentFormat.QUIZ) {
            // QUIZ: chỉ cho QUIZ section
            if (section.getStudyType() != ContentType.QUIZ) {
                throw bad("QUIZ content format only allowed in QUIZ sections");
            }
            // QUIZ content phải có quizId
            if (r.getQuizId() == null) {
                throw bad("quizId is required for QUIZ content format");
            }
            // Verify quiz exists and belongs to this section
            Quiz quiz = quizRepo.findById(r.getQuizId())
                    .orElseThrow(() -> bad("Quiz not found: " + r.getQuizId()));
            if (!quiz.getSection().getId().equals(section.getId())) {
                throw bad("Quiz does not belong to this section");
            }
            // QUIZ content không được primary
            if (r.isPrimaryContent()) {
                throw bad("primaryContent must be false for QUIZ content");
            }
            // Check if section already has a QUIZ content (only 1 QUIZ content per section)
            long quizContentCount = section.getContents().stream()
                    .filter(sc -> excludeContentId == null || !sc.getId().equals(excludeContentId))
                    .filter(sc -> sc.getContentFormat() == ContentFormat.QUIZ)
                    .count();
            if (quizContentCount >= 1) {
                throw bad("QUIZ section can only have ONE QUIZ content");
            }

        } else {
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

        } else if (s.getStudyType() == ContentType.QUIZ) {
            // QUIZ section must have a quiz
            Optional<Quiz> quizOpt = quizRepo.findBySection_Id(s.getId());
            if (quizOpt.isEmpty() || quizOpt.get().getDeletedFlag() == Boolean.TRUE) {
                throw bad("QUIZ section must have a quiz");
            }
            Quiz quiz = quizOpt.get();
            if (quiz.getTotalQuestions() == null || quiz.getTotalQuestions() < 1) {
                throw bad("QUIZ section must have at least one question");
            }
            // QUIZ section must have a QUIZ content format for progress tracking
            boolean hasQuizContent = s.getContents().stream()
                    .anyMatch(sc -> sc.getContentFormat() == ContentFormat.QUIZ && Boolean.TRUE.equals(sc.getIsTrackable()));
            if (!hasQuizContent) {
                throw bad("QUIZ section must have at least one QUIZ content format (for progress tracking)");
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
        res.setTeacherName(getTeacherName(c.getUserId()));

        // ✅ luôn trả enrollCount
        long enrollCount = enrollmentRepo.countByCourseId(c.getId());
        res.setEnrollCount(enrollCount);

        // Map rejection info (chỉ có khi status = REJECTED)
        if (c.getStatus() == CourseStatus.REJECTED) {
            res.setRejectionReason(c.getRejectionReason());
            res.setRejectedAt(c.getRejectedAt());
            res.setRejectedByUserId(c.getRejectedByUserId());
            if (c.getRejectedByUserId() != null) {
                res.setRejectedByUserName(getTeacherName(c.getRejectedByUserId()));
            }
        }
        
        // Map flag info (chỉ có khi status = FLAGGED)
        if (c.getStatus() == CourseStatus.FLAGGED) {
            res.setFlaggedReason(c.getFlaggedReason());
            res.setFlaggedAt(c.getFlaggedAt());
            res.setFlaggedByUserId(c.getFlaggedByUserId());
            if (c.getFlaggedByUserId() != null) {
                res.setFlaggedByUserName(getTeacherName(c.getFlaggedByUserId()));
            }
            // Message thân thiện cho learner (không gây tiêu cực) - chỉ hiển thị cho enrolled learners
            res.setStatusMessage("Khóa học đang được cập nhật. Bạn vẫn có thể học tập bình thường.");
        }
        
        // Set canFlag cho moderator: chỉ có thể flag course có status = PUBLISHED
        res.setCanFlag(c.getStatus() == CourseStatus.PUBLISHED);
        
        // Set isModeratorFlagged: true nếu course đã được moderator flag (đã gửi thông báo cho teacher)
        res.setIsModeratorFlagged(c.getStatus() == CourseStatus.FLAGGED && c.getFlaggedByUserId() != null);

        res.setChapters(List.of());
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
        res.setTeacherName(getTeacherName(c.getUserId()));
        res.setChapters(chapters);
        
        // Map rejection info (chỉ có khi status = REJECTED)
        if (c.getStatus() == CourseStatus.REJECTED) {
            res.setRejectionReason(c.getRejectionReason());
            res.setRejectedAt(c.getRejectedAt());
            res.setRejectedByUserId(c.getRejectedByUserId());
            if (c.getRejectedByUserId() != null) {
                res.setRejectedByUserName(getTeacherName(c.getRejectedByUserId()));
            }
        }
        
        return res;
    }

    private CourseRes toCourseResWithChapters(Course c, List<Chapter> include) {
        List<ChapterRes> chapters = include.stream()
                .sorted(Comparator.comparing(Chapter::getOrderIndex))
                .map(ch -> new ChapterRes(
                        ch.getId(), ch.getTitle(), ch.getOrderIndex(), ch.getSummary(), ch.isTrial(),
                        ch.getLessons().stream()
                                .sorted(Comparator.comparing(Lesson::getOrderIndex))
                                .map(this::toLessonResFull)
                                .collect(Collectors.toList())
                )).collect(Collectors.toList());

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
        res.setTeacherName(getTeacherName(c.getUserId()));
        res.setChapters(chapters);
        
        // Map rejection info (chỉ có khi status = REJECTED)
        if (c.getStatus() == CourseStatus.REJECTED) {
            res.setRejectionReason(c.getRejectionReason());
            res.setRejectedAt(c.getRejectedAt());
            res.setRejectedByUserId(c.getRejectedByUserId());
            if (c.getRejectedByUserId() != null) {
                res.setRejectedByUserName(getTeacherName(c.getRejectedByUserId()));
            }
        }
        
        return res;
    }

    private ChapterRes toChapterResShallow(Chapter ch) {
        return new ChapterRes(ch.getId(), ch.getTitle(), ch.getOrderIndex(), ch.getSummary(), ch.isTrial(), List.of());
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
                .filter(ct -> {
                    // Filter out content with deleted quizId or flashcardSetId
                    if (ct.getQuizId() != null) {
                        Optional<Quiz> quizOpt = quizRepo.findById(ct.getQuizId());
                        if (quizOpt.isEmpty() || Boolean.TRUE.equals(quizOpt.get().getDeletedFlag())) {
                            return false; // Skip content with deleted quiz
                        }
                    }
                    if (ct.getFlashcardSetId() != null) {
                        Optional<FlashcardSet> setOpt = flashcardSetRepo.findById(ct.getFlashcardSetId());
                        if (setOpt.isEmpty() || setOpt.get().isDeletedFlag()) {
                            return false; // Skip content with deleted flashcard set
                        }
                    }
                    return true;
                })
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
        
        // Quiz no longer belongs to lesson, it belongs to section now
        // So quizId is null at lesson level (for backward compatibility)
        Long quizId = null;
        
        LessonRes lessonRes = new LessonRes(
                ls.getId(),
                ls.getTitle(),
                ls.getOrderIndex(),
                ls.getTotalDurationSec(),
                sections
        );
        lessonRes.setQuizId(quizId);
        return lessonRes;
    }

    private ContentRes toContentRes(SectionsContent c) {
        return new ContentRes(
                c.getId(),
                c.getOrderIndex(),
                c.getContentFormat(),
                c.isPrimaryContent(),
                c.getFilePath(),
                c.getRichText(),
                c.getFlashcardSetId(),
                c.getQuizId()
        );
    }

    // =========================
    // COURSE DETAIL (metadata only)
    // =========================
    @Transactional(readOnly = true)
    public CourseRes getDetail(Long id, Long teacherUserId) {
        Course c = getOwned(id, teacherUserId);
        CourseRes res = toCourseResLite(c);

        // nếu CourseRes có field enrollCount
        long enrollCount = enrollmentRepo.countByCourseId(id);
        res.setEnrollCount(enrollCount);

        return res;
    }


    // =========================
    // CHAPTER: update / delete / reorder
    // =========================
    public ChapterRes updateChapter(Long chapterId, Long teacherUserId, ChapterUpsertReq r) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        assertOwner(ch.getCourse().getId(), teacherUserId);

        Long courseId = ch.getCourse().getId();
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Lưu giá trị cũ để detect trial chapter changes
        boolean wasTrial = ch.isTrial();
        boolean trialChanged = false;

        if (r.getTitle() != null) ch.setTitle(r.getTitle());
        ch.setSummary(r.getSummary());
        
        // Nếu update orderIndex về 0, chapter đó phải là trial
        if (r.getOrderIndex() != null && r.getOrderIndex() == 0) {
            // Bỏ trial của chapter trial cũ
            chapterRepo.findByCourse_IdAndIsTrialTrue(courseId).ifPresent(old -> {
                if (!old.getId().equals(ch.getId())) old.setTrial(false);
            });
            trialChanged = !wasTrial; // Detect nếu trial status thay đổi từ false → true
            ch.setTrial(true);
            ch.setOrderIndex(0);
        } else if (Boolean.TRUE.equals(r.getIsTrial())) {
            // Nếu teacher muốn set trial cho chapter khác (không phải đầu tiên)
            if (ch.getOrderIndex() != 0) {
                throw bad("Only the first chapter (orderIndex=0) can be a trial chapter. Please reorder this chapter to position 0 first.");
            }
            chapterRepo.findByCourse_IdAndIsTrialTrue(courseId).ifPresent(old -> {
                if (!old.getId().equals(ch.getId())) old.setTrial(false);
            });
            trialChanged = !wasTrial; // Detect nếu trial status thay đổi từ false → true
            ch.setTrial(true);
        }
        
        chapterRepo.save(ch);
        
        // Đảm bảo chapter đầu tiên luôn là trial
        ensureFirstChapterIsTrial(courseId);
        
        // BR-03: Nếu course đang PUBLISHED và có thay đổi trial chapter → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED && trialChanged) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "trial chapter");
        }
        
        return toChapterResShallow(ch);
    }

    public void deleteChapter(Long chapterId, Long teacherUserId) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        assertOwner(ch.getCourse().getId(), teacherUserId);

        Long courseId = ch.getCourse().getId();
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        chapterRepo.delete(ch);
        renormalizeChapterOrder(courseId);
        
        // Đảm bảo chapter đầu tiên luôn là trial (sau khi xóa và renormalize)
        ensureFirstChapterIsTrial(courseId);
        
        // BR-03: Nếu course đang PUBLISHED và xóa chapter (thay đổi syllabus structure) → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (chapter deleted)");
        }
    }

    public ChapterRes reorderChapter(Long chapterId, Long teacherUserId, int newIndex) {
        Chapter ch = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        assertOwner(ch.getCourse().getId(), teacherUserId);

        Long courseId = ch.getCourse().getId();
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        List<Chapter> list = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
        applyReorder(list, chapterId, newIndex, (it, idx) -> it.setOrderIndex(idx));
        
        // Đảm bảo chapter đầu tiên luôn là trial (sau khi reorder)
        ensureFirstChapterIsTrial(courseId);
        
        // BR-03: Nếu course đang PUBLISHED và reorder chapter (thay đổi syllabus structure) → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (chapter reordered)");
        }
        
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
        
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Long chapterId = ls.getChapter().getId();
        lessonRepo.delete(ls);
        renormalizeLessonOrder(chapterId);
        
        // BR-03: Nếu course đang PUBLISHED và xóa lesson (thay đổi syllabus structure) → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (lesson deleted)");
        }
    }

    public LessonRes reorderLesson(Long lessonId, Long teacherUserId, int newIndex) {
        Lesson ls = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        Long courseId = ls.getChapter().getCourse().getId();
        assertOwner(courseId, teacherUserId);
        
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<Lesson> list = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(ls.getChapter().getId());
        applyReorder(list, lessonId, newIndex, (it, idx) -> it.setOrderIndex(idx));
        
        // BR-03: Nếu course đang PUBLISHED và reorder lesson (thay đổi syllabus structure) → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (lesson reordered)");
        }
        
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
        
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Before deleting section, handle quiz if exists
        // Find quiz for this section (including soft-deleted ones for cleanup)
        Optional<Quiz> quizOpt = quizRepo.findBySection_Id(sectionId);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            
            // Hard delete SectionsContent entries that reference this quiz first
            // This prevents orphaned content references
            List<SectionsContent> quizContents = contentRepo.findBySection_IdOrderByOrderIndexAsc(sectionId)
                    .stream()
                    .filter(sc -> sc.getContentFormat() == ContentFormat.QUIZ 
                            && quiz.getId().equals(sc.getQuizId()))
                    .toList();
            for (SectionsContent content : quizContents) {
                contentRepo.delete(content);
            }
            
            // Hard delete quiz, questions, and options to break foreign key constraint before deleting section
            // Note: section_id is NOT NULL, so we must delete quiz before deleting section
            // Learner attempts (QuizAttempt, QuizAnswer) are preserved as they reference quiz by ID (not FK)
            Long quizId = quiz.getId();
            
            // Delete all options first (they reference questions)
            questionRepo.findByQuiz_IdOrderByOrderIndexAsc(quizId)
                    .forEach(question -> {
                        optionRepo.findByQuestion_IdOrderByOrderIndexAsc(question.getId())
                                .forEach(optionRepo::delete);
                    });
            
            // Delete all questions (they reference quiz)
            questionRepo.findByQuiz_IdOrderByOrderIndexAsc(quizId)
                    .forEach(questionRepo::delete);
            
            // Finally delete quiz (breaks foreign key constraint to section)
            quizRepo.delete(quiz);
        }

        // Before deleting section, handle flashcard sets if exist
        // Find all flashcard sets referenced by sections content in this section
        List<SectionsContent> sectionContents = contentRepo.findBySection_IdOrderByOrderIndexAsc(sectionId);
        Set<Long> processedFlashcardSetIds = new HashSet<>(); // Avoid processing same set multiple times
        for (SectionsContent content : sectionContents) {
            if (content.getFlashcardSetId() != null && !processedFlashcardSetIds.contains(content.getFlashcardSetId())) {
                // Use eager fetch to load cards
                Optional<FlashcardSet> flashcardSetOpt = flashcardSetRepo.findByIdWithCreatedByAndCards(content.getFlashcardSetId());
                if (flashcardSetOpt.isPresent()) {
                    FlashcardSet flashcardSet = flashcardSetOpt.get();
                    // Soft delete flashcard set and its cards
                    // This preserves learner progress history
                    flashcardSet.setDeletedFlag(true);
                    // Soft delete all cards in the set (cards already eager fetched)
                    if (flashcardSet.getCards() != null) {
                        for (com.hokori.web.entity.Flashcard card : flashcardSet.getCards()) {
                            if (!card.isDeletedFlag()) {
                                card.setDeletedFlag(true);
                            }
                        }
                    }
                    flashcardSetRepo.save(flashcardSet);
                    
                    // Hard delete SectionsContent entries that reference this flashcard set
                    // This prevents orphaned content references
                    List<SectionsContent> flashcardContents = contentRepo.findByFlashcardSetId(content.getFlashcardSetId());
                    for (SectionsContent flashcardContent : flashcardContents) {
                        contentRepo.delete(flashcardContent);
                    }
                    
                    processedFlashcardSetIds.add(content.getFlashcardSetId());
                }
            }
        }

        Long lessonId = s.getLesson().getId();
        sectionRepo.delete(s);
        renormalizeSectionOrder(lessonId);
        
        // BR-03: Nếu course đang PUBLISHED và xóa section (thay đổi syllabus structure) → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (section deleted)");
        }
    }

    public SectionRes reorderSection(Long sectionId, Long teacherUserId, int newIndex) {
        Section s = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        Long courseId = s.getLesson().getChapter().getCourse().getId();
        assertOwner(courseId, teacherUserId);
        
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<Section> list = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(s.getLesson().getId());
        applyReorder(list, sectionId, newIndex, (it, idx) -> it.setOrderIndex(idx));
        
        // BR-03: Nếu course đang PUBLISHED và reorder section (thay đổi syllabus structure) → auto-submit
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            autoSubmitForApprovalIfPublished(course, teacherUserId, "syllabus structure (section reordered)");
        }
        
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

        // validate theo section hiện tại, exclude chính content đang update
        validateContentPayload(c.getSection(), r, contentId);

        // Xóa file cũ nếu filePath thay đổi và content có filePath cũ
        String oldFilePath = c.getFilePath();
        String newFilePath = r.getFilePath();
        if (oldFilePath != null && !oldFilePath.equals(newFilePath) && newFilePath != null) {
            // FilePath đã thay đổi, xóa file cũ
            try {
                fileStorageService.deleteFile(oldFilePath);
            } catch (Exception e) {
                // Log error nhưng không throw để không block update
                // File có thể đã bị xóa hoặc không tồn tại
                System.err.println("Warning: Could not delete old file: " + oldFilePath + " - " + e.getMessage());
            }
        }

        if (r.getContentFormat() != null) {
            c.setContentFormat(r.getContentFormat());
        }
        c.setPrimaryContent(r.isPrimaryContent());
        c.setFilePath(r.getFilePath());
        c.setRichText(r.getRichText());
        c.setFlashcardSetId(r.getFlashcardSetId());
        c.setQuizId(r.getQuizId());

        return toContentRes(c);
    }

    public void deleteContent(Long contentId, Long teacherUserId) {
        SectionsContent c = contentRepo.findById(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        Long courseId = c.getSection().getLesson().getChapter().getCourse().getId();
        assertOwner(courseId, teacherUserId);

        // Xóa file vật lý nếu content có filePath
        String filePath = c.getFilePath();
        if (filePath != null && !filePath.trim().isEmpty()) {
            try {
                fileStorageService.deleteFile(filePath);
            } catch (Exception e) {
                // Log error nhưng không throw để không block delete
                // File có thể đã bị xóa hoặc không tồn tại
                System.err.println("Warning: Could not delete file: " + filePath + " - " + e.getMessage());
            }
        }

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

    /**
     * Đảm bảo chapter đầu tiên (orderIndex = 0) luôn là trial chapter.
     * Nếu chapter đầu tiên không phải trial, tự động set thành trial.
     * Nếu có chapter trial khác, bỏ trial của nó.
     */
    private void ensureFirstChapterIsTrial(Long courseId) {
        List<Chapter> chapters = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
        if (chapters.isEmpty()) {
            return; // Không có chapter nào, không cần làm gì
        }
        
        Chapter firstChapter = chapters.get(0);
        
        // Nếu chapter đầu tiên chưa phải trial, set thành trial
        if (!firstChapter.isTrial()) {
            // Bỏ trial của chapter trial cũ (nếu có)
            chapterRepo.findByCourse_IdAndIsTrialTrue(courseId).ifPresent(old -> {
                if (!old.getId().equals(firstChapter.getId())) {
                    old.setTrial(false);
                }
            });
            firstChapter.setTrial(true);
            chapterRepo.save(firstChapter);
        } else {
            // Nếu chapter đầu tiên đã là trial, đảm bảo không có chapter trial nào khác
            chapterRepo.findByCourse_IdAndIsTrialTrue(courseId).ifPresent(old -> {
                if (!old.getId().equals(firstChapter.getId())) {
                    old.setTrial(false);
                    chapterRepo.save(old);
                }
            });
        }
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
        return getPublishedTree(courseId, null);
    }

    @Transactional(readOnly = true)
    public CourseRes getPublishedTree(Long courseId, Long userId) {
        // Use native query to check status without loading LOB fields
        Object[] metadata = courseRepo.findCourseMetadataById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Handle nested array case (PostgreSQL)
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }

        // Validate array length
        if (actualMetadata.length < 10) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                String.format("Invalid course metadata: array too short (expected >= 10, got %d) for course %d", 
                    actualMetadata.length, courseId));
        }
        
        // Validate status field (index 9)
        if (actualMetadata[9] == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                String.format("Invalid course metadata: missing status (status is null) for course %d. " +
                    "This may indicate a data integrity issue. Please check course status in database.", courseId));
        }

        // Check status (at index 9)
        CourseStatus status;
        try {
            status = CourseStatus.valueOf(actualMetadata[9].toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid course status: " + actualMetadata[9]);
        }
        
        // Check if user is enrolled (for FLAGGED courses access)
        boolean isEnrolled = userId != null && enrollmentRepo.existsByUserIdAndCourseId(userId, courseId);
        
        if (status != CourseStatus.PUBLISHED && status != CourseStatus.PENDING_UPDATE) {
            // Nếu course bị FLAGGED: chỉ cho phép access nếu user đã enrolled
            if (status == CourseStatus.FLAGGED) {
                if (!isEnrolled) {
                    // User chưa enrolled → ẩn course như không tồn tại
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course has been flagged and is temporarily unavailable");
                }
                // User đã enrolled → cho phép access (đã trả tiền)
            } else {
                // Các status khác (DRAFT, PENDING_APPROVAL, REJECTED, ARCHIVED) → reject
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course is not published");
            }
        }
        
        // Nếu status = PENDING_UPDATE, dùng snapshot để hiển thị nội dung CŨ cho learners
        CourseRes res;
        if (status == CourseStatus.PENDING_UPDATE) {
            // Load course entity to get snapshot
            Course course = courseRepo.findByIdAndDeletedFlagFalse(courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
            
            if (course.getSnapshotData() != null && !course.getSnapshotData().trim().isEmpty()) {
                // Restore from snapshot (old content)
                res = restoreCourseResFromSnapshot(course, course.getSnapshotData());
            } else {
                // Fallback: use current content if snapshot not available
                res = getTree(courseId);
            }
        } else {
            // PUBLISHED: use current content
            res = getTree(courseId);
        }
        long enrollCount = enrollmentRepo.countByCourseId(courseId);
        res.setEnrollCount(enrollCount);
        
        // Set isEnrolled if userId provided
        if (userId != null) {
            res.setIsEnrolled(isEnrolled);
        }
        
        return res;
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

    /**
     * Check if a lesson belongs to a course that is pending approval (for moderator access)
     */
    @Transactional(readOnly = true)
    public void requireLessonBelongsToPendingApprovalCourse(Long lessonId) {
        Long courseId = lessonRepo.findCourseIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        
        // Check course status
        Object[] metadata = courseRepo.findCourseMetadataById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }

        if (actualMetadata.length < 10 || actualMetadata[9] == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid course metadata: missing status");
        }

        CourseStatus status;
        try {
            status = CourseStatus.valueOf(actualMetadata[9].toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid course status: " + actualMetadata[9]);
        }
        
        if (status != CourseStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Lesson does not belong to a course pending approval. Current status: " + status);
        }
    }

    /**
     * Check if a section content belongs to a course that is pending approval (for moderator access)
     */
    @Transactional(readOnly = true)
    public void requireSectionContentBelongsToPendingApprovalCourse(Long sectionContentId) {
        // Get section ID from section content
        SectionsContent content = contentRepo.findById(sectionContentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section content not found"));
        
        Section section = content.getSection();
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found");
        }
        
        // Get lesson from section
        Lesson lesson = section.getLesson();
        if (lesson == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        
        // Check course status via lesson
        requireLessonBelongsToPendingApprovalCourse(lesson.getId());
    }

    // =========================
    // SNAPSHOT METHODS (for PENDING_UPDATE)
    // =========================

    /**
     * Create snapshot of course tree (serialize to JSON).
     * Used to preserve old content when course is in PENDING_UPDATE status.
     */
    private String createSnapshot(CourseRes courseRes) {
        try {
            return objectMapper.writeValueAsString(courseRes);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to create course snapshot: " + e.getMessage());
        }
    }

    /**
     * Restore course tree from snapshot (deserialize from JSON).
     * Used to display old content to learners when course is in PENDING_UPDATE status.
     * 
     * Note: This returns CourseRes for display purposes only.
     * To actually restore course entities, use restoreCourseEntitiesFromSnapshot(Course, String).
     */
    private CourseRes restoreCourseResFromSnapshot(Course course, String snapshotJson) {
        try {
            CourseRes snapshot = objectMapper.readValue(snapshotJson, CourseRes.class);
            // Update basic course info from current entity (status, metadata, etc.)
            snapshot.setId(course.getId());
            snapshot.setStatus(course.getStatus());
            snapshot.setPublishedAt(course.getPublishedAt());
            snapshot.setPendingUpdateAt(course.getPendingUpdateAt());
            snapshot.setEnrollCount(enrollmentRepo.countByCourseId(course.getId()));
            snapshot.setTeacherName(getTeacherName(course.getUserId()));
            return snapshot;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to restore course from snapshot: " + e.getMessage());
        }
    }

    /**
     * Restore course entities from snapshot (actually modify database).
     * Used when moderator rejects update - revert course tree to old content.
     * 
     * This method deletes current content and recreates from snapshot.
     */
    private void restoreCourseEntitiesFromSnapshot(Course course, String snapshotJson) {
        try {
            CourseRes snapshot = objectMapper.readValue(snapshotJson, CourseRes.class);
            Long courseId = course.getId();
            
            // Delete all current chapters (cascade will delete lessons, sections, contents)
            List<Chapter> currentChapters = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
            chapterRepo.deleteAll(currentChapters);
            
            // Recreate chapters from snapshot
            if (snapshot.getChapters() != null) {
                for (ChapterRes chapterRes : snapshot.getChapters()) {
                    Chapter chapter = new Chapter();
                    chapter.setCourse(course);
                    chapter.setTitle(chapterRes.getTitle());
                    chapter.setSummary(chapterRes.getSummary());
                    chapter.setOrderIndex(chapterRes.getOrderIndex());
                    chapter.setTrial(chapterRes.getIsTrial());
                    chapter = chapterRepo.save(chapter);
                    
                    // Recreate lessons
                    if (chapterRes.getLessons() != null) {
                        for (LessonRes lessonRes : chapterRes.getLessons()) {
                            Lesson lesson = new Lesson();
                            lesson.setChapter(chapter);
                            lesson.setTitle(lessonRes.getTitle());
                            lesson.setOrderIndex(lessonRes.getOrderIndex());
                            lesson.setTotalDurationSec(lessonRes.getTotalDurationSec());
                            lesson = lessonRepo.save(lesson);
                            
                            // Recreate sections
                            if (lessonRes.getSections() != null) {
                                for (SectionRes sectionRes : lessonRes.getSections()) {
                                    Section section = new Section();
                                    section.setLesson(lesson);
                                    section.setTitle(sectionRes.getTitle());
                                    section.setOrderIndex(sectionRes.getOrderIndex());
                                    section.setStudyType(sectionRes.getStudyType());
                                    section.setFlashcardSetId(sectionRes.getFlashcardSetId());
                                    section = sectionRepo.save(section);
                                    
                                    // Recreate contents
                                    if (sectionRes.getContents() != null) {
                                        for (ContentRes contentRes : sectionRes.getContents()) {
                                            SectionsContent content = new SectionsContent();
                                            content.setSection(section);
                                            content.setOrderIndex(contentRes.getOrderIndex());
                                            content.setContentFormat(contentRes.getContentFormat());
                                            content.setPrimaryContent(contentRes.isPrimaryContent());
                                            content.setFilePath(contentRes.getFilePath());
                                            content.setRichText(contentRes.getRichText());
                                            content.setFlashcardSetId(contentRes.getFlashcardSetId());
                                            content.setQuizId(contentRes.getQuizId());
                                            contentRepo.save(content);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to restore course entities from snapshot: " + e.getMessage());
        }
    }
}
