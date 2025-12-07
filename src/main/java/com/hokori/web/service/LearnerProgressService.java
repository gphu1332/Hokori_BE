package com.hokori.web.service;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.dto.course.*;
import com.hokori.web.dto.flashcard.FlashcardSetResponse;
import com.hokori.web.dto.progress.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.ZoneId;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LearnerProgressService {

    private final EnrollmentRepository enrollmentRepo;
    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final LessonRepository lessonRepo;
    private final SectionRepository sectionRepo;
    private final SectionsContentRepository contentRepo;
    private final UserContentProgressRepository ucpRepo;
    private final QuizRepository quizRepo;
    private final FlashcardSetRepository flashcardSetRepo;
    private final UserDailyLearningRepository userDailyLearningRepo;

    // ================= Enrollment =================
    
    /**
     * Enroll learner into a course
     */
    public EnrollmentLiteRes enrollCourse(Long userId, Long courseId) {
        // Check if already enrolled
        if (enrollmentRepo.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this course");
        }
        
        // Check if course exists and is PUBLISHED (use native query to avoid LOB stream error)
        var courseMetadataOpt = courseRepo.findCourseMetadataById(courseId);
        if (courseMetadataOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        
        Object[] metadata = courseMetadataOpt.get();
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course is not published");
        }
        
        // Check if course is free or paid
        // Metadata: [id, title, slug, subtitle, level, priceCents, discountedPriceCents, currency, coverImagePath, status, publishedAt, userId, deletedFlag, teacherName]
        Long priceCents = actualMetadata[5] != null ? ((Number) actualMetadata[5]).longValue() : null;
        Long discountedPriceCents = actualMetadata[6] != null ? ((Number) actualMetadata[6]).longValue() : null;
        
        // Determine final price (use discounted price if available, otherwise regular price)
        Long finalPrice = (discountedPriceCents != null && discountedPriceCents > 0) ? discountedPriceCents : priceCents;
        
        // If course has a price > 0, user must pay first (enroll via payment flow)
        if (finalPrice != null && finalPrice > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "This course requires payment. Please add it to cart and checkout.");
        }
        
        // Course is free (priceCents == null or priceCents == 0), allow direct enrollment
        // Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .courseId(courseId)
                .progressPercent(0)
                .startedAt(Instant.now())
                .lastAccessAt(Instant.now())
                .build();
        
        Enrollment saved = enrollmentRepo.save(enrollment);
        
        return EnrollmentLiteRes.builder()
                .enrollmentId(saved.getId())
                .courseId(saved.getCourseId())
                .progressPercent(saved.getProgressPercent())
                .startedAt(saved.getStartedAt())
                .completedAt(saved.getCompletedAt())
                .lastAccessAt(saved.getLastAccessAt())
                .build();
    }
    
    /**
     * Enroll learner into a course AFTER successful payment
     * This method skips price check since payment has already been made
     */
    public EnrollmentLiteRes enrollCourseAfterPayment(Long userId, Long courseId) {
        // Check if already enrolled
        if (enrollmentRepo.existsByUserIdAndCourseId(userId, courseId)) {
            // Already enrolled, return existing enrollment
            Enrollment existing = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Enrollment check failed"));
            return EnrollmentLiteRes.builder()
                    .enrollmentId(existing.getId())
                    .courseId(existing.getCourseId())
                    .progressPercent(existing.getProgressPercent())
                    .startedAt(existing.getStartedAt())
                    .completedAt(existing.getCompletedAt())
                    .lastAccessAt(existing.getLastAccessAt())
                    .build();
        }
        
        // Check if course exists and is PUBLISHED (use native query to avoid LOB stream error)
        var courseMetadataOpt = courseRepo.findCourseMetadataById(courseId);
        if (courseMetadataOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        
        Object[] metadata = courseMetadataOpt.get();
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Course is not published");
        }
        
        // Skip price check - user has already paid
        // Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .courseId(courseId)
                .progressPercent(0)
                .startedAt(Instant.now())
                .lastAccessAt(Instant.now())
                .build();
        
        Enrollment saved = enrollmentRepo.save(enrollment);
        
        return EnrollmentLiteRes.builder()
                .enrollmentId(saved.getId())
                .courseId(saved.getCourseId())
                .progressPercent(saved.getProgressPercent())
                .startedAt(saved.getStartedAt())
                .completedAt(saved.getCompletedAt())
                .lastAccessAt(saved.getLastAccessAt())
                .build();
    }
    
    @Transactional(readOnly = true)
    public EnrollmentLiteRes getEnrollment(Long userId, Long courseId) {
        Enrollment e = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled"));
        return EnrollmentLiteRes.builder()
                .enrollmentId(e.getId())
                .courseId(e.getCourseId())
                .progressPercent(e.getProgressPercent())
                .startedAt(e.getStartedAt())
                .completedAt(e.getCompletedAt())
                .lastAccessAt(e.getLastAccessAt())
                .build();
    }
    
    /**
     * List all enrolled courses for a learner
     */
    @Transactional(readOnly = true)
    public List<EnrollmentLiteRes> listEnrolledCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentRepo.findByUserId(userId);
        return enrollments.stream()
                .map(e -> EnrollmentLiteRes.builder()
                        .enrollmentId(e.getId())
                        .courseId(e.getCourseId())
                        .progressPercent(e.getProgressPercent())
                        .startedAt(e.getStartedAt())
                        .completedAt(e.getCompletedAt())
                        .lastAccessAt(e.getLastAccessAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ================= Chapter % =================
    @Transactional(readOnly = true)
    public List<ChapterProgressRes> getChaptersProgress(Long userId, Long courseId) {
        Enrollment e = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled"));

        List<Chapter> chapters = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
        List<ChapterProgressRes> result = new ArrayList<>(chapters.size());

        for (Chapter ch : chapters) {
            // collect contents of this chapter
            List<Long> contentIds = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(ch.getId()).stream()
                    .flatMap(ls -> sectionRepo.findByLesson_IdOrderByOrderIndexAsc(ls.getId()).stream())
                    .flatMap(s -> contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId()).stream())
                    .filter(c -> Boolean.TRUE.equals(c.getIsTrackable()))
                    .map(SectionsContent::getId).toList();

            long total = contentIds.size();
            long completed = total == 0 ? 0 : ucpRepo.countCompletedInList(e.getId(), contentIds);
            int percent = (total == 0) ? 100 : Math.toIntExact(Math.round(100.0 * completed / total));

            // stats (đơn giản: video/exercise/test); tùy enum bạn map
            int videos = 0, exercises = 0, tests = 0;
            long duration = 0L;
            // nếu bạn có Asset.durationSec, có thể join thêm ở đây – để 0 tạm thời

            result.add(ChapterProgressRes.builder()
                    .chapterId(ch.getId())
                    .title(ch.getTitle())
                    .orderIndex(ch.getOrderIndex())
                    .percent(percent)
                    .stats(ChapterProgressRes.Stats.builder()
                            .videos(videos).exercises(exercises).tests(tests)
                            .totalDurationSec(duration).build())
                    .build());
        }
        return result;
    }

    // ================= Lesson ✓ =================
    @Transactional(readOnly = true)
    public List<LessonProgressRes> getLessonsProgress(Long userId, Long courseId) {
        Enrollment e = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled"));

        List<Lesson> lessons = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId).stream()
                .flatMap(ch -> lessonRepo.findByChapter_IdOrderByOrderIndexAsc(ch.getId()).stream())
                .collect(Collectors.toList());

        List<LessonProgressRes> res = new ArrayList<>(lessons.size());
        for (Lesson ls : lessons) {
            List<Long> contentIds = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(ls.getId()).stream()
                    .flatMap(s -> contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId()).stream())
                    .filter(c -> Boolean.TRUE.equals(c.getIsTrackable()))
                    .map(SectionsContent::getId).toList();

            long total = contentIds.size();
            long completed = (total == 0) ? 0 : ucpRepo.countCompletedInList(e.getId(), contentIds);
            boolean isCompleted = (total == 0) || (completed == total);

            res.add(LessonProgressRes.builder()
                    .lessonId(ls.getId())
                    .title(ls.getTitle())
                    .orderIndex(ls.getOrderIndex())
                    .isCompleted(isCompleted)
                    .build());
        }
        return res;
    }

    // ============== Contents of a lesson (with progress) ==============
    @Transactional(readOnly = true)
    public List<ContentProgressRes> getLessonContentsProgress(Long userId, Long lessonId) {
        Long courseId = lessonRepo.findCourseIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        
        Long chapterId = lessonRepo.findChapterIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found for lesson"));

        // Check if lesson belongs to trial chapter
        Chapter chapter = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        
        boolean isTrialChapter = chapter.isTrial();
        
        Enrollment e = null;
        Map<Long, UserContentProgress> ucpMap = new HashMap<>();
        
        // Get all contents for this lesson first
        List<SectionsContent> contents = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lessonId).stream()
                .flatMap(s -> contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId()).stream())
                .collect(Collectors.toList());
        
        // If not trial chapter, require enrollment and get progress
        if (!isTrialChapter) {
            e = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled"));
            
            List<Long> contentIds = contents.stream().map(SectionsContent::getId).toList();
            
            // Use JOIN FETCH query to eagerly load content to avoid lazy loading issues
            // Query all progress for this enrollment and content IDs
            List<UserContentProgress> ucpList = ucpRepo
                    .findByEnrollment_IdAndContent_IdInWithContent(e.getId(), contentIds);
            
            // Build map: key is content.id, value is UserContentProgress
            // Important: Use ucp.getContent().getId() as key because content is eagerly fetched
            ucpMap = ucpList.stream()
                    .collect(Collectors.toMap(
                            ucp -> ucp.getContent().getId(), 
                            ucp -> ucp,
                            (existing, replacement) -> existing // Keep first if duplicate keys
                    ));
        }
        // If trial chapter, allow access without enrollment (no progress tracking)

        List<ContentProgressRes> res = new ArrayList<>(contents.size());
        for (SectionsContent c : contents) {
            UserContentProgress up = ucpMap.get(c.getId());
            res.add(ContentProgressRes.builder()
                    .contentId(c.getId())
                    .contentFormat(c.getContentFormat())
                    .isTrackable(Boolean.TRUE.equals(c.getIsTrackable()))
                    .lastPositionSec(up == null ? null : up.getLastPositionSec())
                    .isCompleted(up != null && Boolean.TRUE.equals(up.getIsCompleted()))
                    .durationSec(null) // nếu cần lấy từ Asset thì map thêm
                    .build());
        }
        return res;
    }

    // ============== Update one content progress ======================
    @Transactional
    public ContentProgressRes updateContentProgress(Long userId, Long contentId, ContentProgressUpsertReq req) {
        SectionsContent content = contentRepo.findById(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));

        Long lessonId = content.getSection().getLesson().getId();
        Long courseId = content.getSection().getLesson().getChapter().getCourse().getId();

        Enrollment e = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled"));

        UserContentProgress ucp = ucpRepo.findByEnrollment_IdAndContent_Id(e.getId(), contentId)
                .orElseGet(() -> UserContentProgress.builder()
                        .enrollment(e).content(content).build());

        if (req.getLastPositionSec() != null) ucp.setLastPositionSec(req.getLastPositionSec());
        if (req.getIsCompleted() != null) {
            ucp.setIsCompleted(req.getIsCompleted());
            ucp.setCompletedAt(Boolean.TRUE.equals(req.getIsCompleted()) ? Instant.now() : null);
        }
        ucpRepo.save(ucp);
        // Flush to ensure data is written to DB immediately
        ucpRepo.flush();

        // update enrollment course percent (recompute simple)
        recomputeCoursePercent(e);

        e.setLastAccessAt(Instant.now());
        enrollmentRepo.save(e);

        recordLearningActivity(userId, Instant.now());

        // return latest content progress
        return ContentProgressRes.builder()
                .contentId(content.getId())
                .contentFormat(content.getContentFormat())
                .isTrackable(Boolean.TRUE.equals(content.getIsTrackable()))
                .lastPositionSec(ucp.getLastPositionSec())
                .isCompleted(ucp.getIsCompleted())
                .durationSec(null)
                .build();
    }

    // ======= helper: recompute course percent across all trackable contents =======
    private void recomputeCoursePercent(Enrollment e) {
        List<Long> allTrackableContentIds = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(e.getCourseId()).stream()
                .flatMap(ch -> lessonRepo.findByChapter_IdOrderByOrderIndexAsc(ch.getId()).stream())
                .flatMap(ls -> sectionRepo.findByLesson_IdOrderByOrderIndexAsc(ls.getId()).stream())
                .flatMap(s -> contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId()).stream())
                .filter(c -> Boolean.TRUE.equals(c.getIsTrackable()))
                .map(SectionsContent::getId).toList();

        long total = allTrackableContentIds.size();
        long completed = (total == 0) ? 0 : ucpRepo.countCompletedInList(e.getId(), allTrackableContentIds);
        int percent = (total == 0) ? 100 : (int)Math.round(100.0 * completed / total);
        e.setProgressPercent(percent);
    }

    // ============== Get Lesson Detail with Full Content (for enrolled learners) ==============
    /**
     * Get lesson detail with sections and contents (filePath, richText) for enrolled learner.
     * Only accessible if learner is enrolled in the course.
     * For trial chapters, allows access without enrollment.
     */
    @Transactional(readOnly = true)
    public LessonRes getLessonDetail(Long userId, Long lessonId) {
        // Get courseId and chapterId from lesson
        Long courseId = lessonRepo.findCourseIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        
        Long chapterId = lessonRepo.findChapterIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found for lesson"));

        // Check if lesson belongs to trial chapter
        Chapter chapter = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        
        boolean isTrialChapter = chapter.isTrial();
        
        // Get enrollment and progress (if not trial chapter)
        Enrollment enrollment = null;
        Map<Long, UserContentProgress> ucpMap = new HashMap<>();
        if (!isTrialChapter) {
            enrollment = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled in this course"));
        }
        // If trial chapter, allow access without enrollment (no progress tracking)

        // Get lesson entity
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        // Get sections with contents
        List<Section> sections = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lessonId);
        
        // Collect all content IDs and query progress at once (if enrolled)
        List<Long> allContentIds = new ArrayList<>();
        for (Section section : sections) {
            List<SectionsContent> contents = contentRepo.findBySection_IdOrderByOrderIndexAsc(section.getId());
            for (SectionsContent content : contents) {
                allContentIds.add(content.getId());
            }
        }
        
        // Query all progress for this lesson at once with JOIN FETCH (if enrolled)
        if (enrollment != null && !allContentIds.isEmpty()) {
            ucpMap = ucpRepo
                    .findByEnrollment_IdAndContent_IdInWithContent(enrollment.getId(), allContentIds)
                    .stream()
                    .collect(Collectors.toMap(ucp -> ucp.getContent().getId(), ucp -> ucp));
        }
        
        List<SectionRes> sectionResList = new ArrayList<>(sections.size());

        for (Section section : sections) {
            List<SectionsContent> contents = contentRepo.findBySection_IdOrderByOrderIndexAsc(section.getId());
            List<ContentRes> contentResList = new ArrayList<>(contents.size());

            for (SectionsContent content : contents) {
                // Get progress for this content
                UserContentProgress ucp = ucpMap.get(content.getId());
                
                // Get progress for this content
                Long lastPositionSec = ucp != null ? ucp.getLastPositionSec() : null;
                Boolean isCompleted = ucp != null && Boolean.TRUE.equals(ucp.getIsCompleted()) ? true : false;
                
                contentResList.add(new ContentRes(
                        content.getId(),
                        content.getOrderIndex(),
                        content.getContentFormat(),
                        content.isPrimaryContent(),
                        content.getFilePath(),
                        content.getRichText(),
                        content.getFlashcardSetId(),
                        lastPositionSec,
                        isCompleted
                ));
            }

            sectionResList.add(new SectionRes(
                    section.getId(),
                    section.getTitle(),
                    section.getOrderIndex(),
                    section.getStudyType(),
                    section.getFlashcardSetId(),
                    contentResList
            ));
        }

        // Get quizId if exists
        Long quizId = quizRepo.findByLesson_Id(lessonId)
                .map(Quiz::getId)
                .orElse(null);

        // Build and return LessonRes
        return new LessonRes(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getOrderIndex(),
                lesson.getTotalDurationSec(),
                sectionResList,
                quizId
        );
    }

    /**
     * Get trial lesson detail (public - no authentication required).
     * Only works for lessons in trial chapters.
     */
    @Transactional(readOnly = true)
    public LessonRes getTrialLessonDetail(Long lessonId) {
        Long chapterId = lessonRepo.findChapterIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found for lesson"));

        // Check if lesson belongs to trial chapter
        Chapter chapter = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        
        if (!chapter.isTrial()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This lesson is not part of trial chapter");
        }

        // Get lesson entity
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        // Get sections with contents
        List<Section> sections = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lessonId);
        List<SectionRes> sectionResList = new ArrayList<>(sections.size());

        for (Section section : sections) {
            List<SectionsContent> contents = contentRepo.findBySection_IdOrderByOrderIndexAsc(section.getId());
            List<ContentRes> contentResList = new ArrayList<>(contents.size());

            for (SectionsContent content : contents) {
                contentResList.add(new ContentRes(
                        content.getId(),
                        content.getOrderIndex(),
                        content.getContentFormat(),
                        content.isPrimaryContent(),
                        content.getFilePath(),
                        content.getRichText(),
                        content.getFlashcardSetId()
                ));
            }

            sectionResList.add(new SectionRes(
                    section.getId(),
                    section.getTitle(),
                    section.getOrderIndex(),
                    section.getStudyType(),
                    section.getFlashcardSetId(),
                    contentResList
            ));
        }

        // Get quizId if exists
        Long quizId = quizRepo.findByLesson_Id(lessonId)
                .map(Quiz::getId)
                .orElse(null);

        // Build and return LessonRes
        return new LessonRes(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getOrderIndex(),
                lesson.getTotalDurationSec(),
                sectionResList,
                quizId
        );
    }

    /**
     * Get trial lesson contents (public - no authentication required).
     * Only works for lessons in trial chapters. No progress tracking.
     */
    @Transactional(readOnly = true)
    public List<ContentProgressRes> getTrialLessonContents(Long lessonId) {
        Long chapterId = lessonRepo.findChapterIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found for lesson"));

        // Check if lesson belongs to trial chapter
        Chapter chapter = chapterRepo.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found"));
        
        if (!chapter.isTrial()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This lesson is not part of trial chapter");
        }

        List<SectionsContent> contents = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lessonId).stream()
                .flatMap(s -> contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId()).stream())
                .collect(Collectors.toList());

        // No progress tracking for trial (guest access)
        List<ContentProgressRes> res = new ArrayList<>(contents.size());
        for (SectionsContent c : contents) {
            res.add(ContentProgressRes.builder()
                    .contentId(c.getId())
                    .contentFormat(c.getContentFormat())
                    .isTrackable(Boolean.TRUE.equals(c.getIsTrackable()))
                    .lastPositionSec(null) // No progress for guest
                    .isCompleted(false) // No progress for guest
                    .durationSec(null)
                    .build());
        }
        return res;
    }

    // ============== Get Flashcard Set for Course Content (for enrolled learners) ==============
    /**
     * Get flashcard set (COURSE_VOCAB) attached to a section content.
     * Only accessible if learner is enrolled in the course.
     */
    @Transactional(readOnly = true)
    public FlashcardSetResponse getFlashcardSetForContent(Long userId, Long sectionContentId) {
        // Get courseId from sectionContent
        Long courseId = contentRepo.findCourseIdBySectionContentId(sectionContentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section content not found"));

        // Check enrollment
        enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "You must enroll in this course to access flashcard sets"));

        // Get flashcard set with eager fetching to avoid LazyInitializationException
        // If multiple sets exist, get the most recent one (ORDER BY createdAt DESC)
        List<FlashcardSet> sets = flashcardSetRepo.findBySectionContent_IdAndDeletedFlagFalseWithCreatedBy(sectionContentId);
        if (sets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Flashcard set not found for this section content");
        }
        // Get the most recent set (first in list due to ORDER BY createdAt DESC)
        FlashcardSet set = sets.get(0);

        return FlashcardSetResponse.fromEntity(set);
    }

    public void recordLearningActivity(Long userId, Instant when) {
        LocalDate date = when.atZone(ZoneId.systemDefault()).toLocalDate();

        userDailyLearningRepo.findByUserIdAndLearningDate(userId, date)
                .ifPresentOrElse(
                        log -> log.setActivityCount(log.getActivityCount() + 1),
                        () -> {
                            UserDailyLearning log = new UserDailyLearning();
                            log.setUserId(userId);
                            log.setLearningDate(date);
                            log.setActivityCount(1);
                            userDailyLearningRepo.save(log);
                        }
                );
    }

    // 🔥 THÊM HÀM NÀY NGAY BÊN DƯỚI
    @Transactional(readOnly = true)
    public int getLearningStreak(Long userId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate from = today.minusDays(60); // ví dụ chỉ cần nhìn 60 ngày gần nhất

        // cần method này trong UserDailyLearningRepository
        List<UserDailyLearning> logs =
                userDailyLearningRepo.findByUserIdAndLearningDateBetweenOrderByLearningDateDesc(
                        userId, from, today);

        if (logs.isEmpty()) {
            return 0;
        }

        // chuyển thành set cho dễ check
        Set<LocalDate> days = logs.stream()
                .map(UserDailyLearning::getLearningDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = today;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }


    @Transactional(readOnly = true)
    public int getCurrentLearningStreak(Long userId) {
        var lastOpt = userDailyLearningRepo.findTopByUserIdOrderByLearningDateDesc(userId);
        if (lastOpt.isEmpty()) {
            return 0; // chưa học ngày nào
        }

        var today = LocalDate.now(ZoneId.systemDefault());
        var lastDate = lastOpt.get().getLearningDate();

        // Nếu ngày học gần nhất < hôm qua -> đã nghỉ ít nhất 1 ngày -> streak = 0
        if (lastDate.isBefore(today.minusDays(1))) {
            return 0;
        }

        // Ngược lại: bắt đầu đếm streak lùi dần từng ngày
        int streak = 0;
        LocalDate cursor = lastDate;

        while (userDailyLearningRepo.existsByUserIdAndLearningDate(userId, cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    // ============== Get Course Learning Tree with Progress (Coursera-style) ==============
    /**
     * Get full course learning tree with progress for enrolled learner.
     * Returns course structure (chapters -> lessons -> sections -> contents) with progress info.
     * Similar to Coursera's course structure view.
     */
    @Transactional(readOnly = true)
    public CourseLearningTreeRes getCourseLearningTree(Long userId, Long courseId) {
        // Check enrollment
        Enrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled in this course"));

        // Get course metadata
        Object[] metadata = courseRepo.findCourseMetadataById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Handle nested array case (PostgreSQL)
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }

        // Extract course info
        Long courseIdFromMeta = ((Number) actualMetadata[0]).longValue();
        String courseTitle = actualMetadata[1] != null ? actualMetadata[1].toString() : "";
        String courseSubtitle = actualMetadata[3] != null ? actualMetadata[3].toString() : null;
        String coverImagePath = actualMetadata[8] != null ? actualMetadata[8].toString() : null;

        // Get chapters with lessons, sections, contents
        List<Chapter> chapters = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
        List<ChapterLearningTreeRes> chapterTrees = new ArrayList<>(chapters.size());

        // Get all progress data upfront for efficiency
        List<ChapterProgressRes> chapterProgressList = getChaptersProgress(userId, courseId);
        Map<Long, ChapterProgressRes> chapterProgressMap = chapterProgressList.stream()
                .collect(Collectors.toMap(ChapterProgressRes::getChapterId, cp -> cp));

        List<LessonProgressRes> lessonProgressList = getLessonsProgress(userId, courseId);
        Map<Long, LessonProgressRes> lessonProgressMap = lessonProgressList.stream()
                .collect(Collectors.toMap(LessonProgressRes::getLessonId, lp -> lp));

        // Get all content progress for entire course at once (more efficient)
        // Collect all contents from all lessons in the course and build content map
        Map<Long, SectionsContent> allContentsMap = new HashMap<>();
        List<Long> allContentIds = new ArrayList<>();
        for (Chapter chapter : chapters) {
            List<Lesson> lessons = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(chapter.getId());
            for (Lesson lesson : lessons) {
                List<Section> sections = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lesson.getId());
                for (Section section : sections) {
                    List<SectionsContent> contents = contentRepo.findBySection_IdOrderByOrderIndexAsc(section.getId());
                    for (SectionsContent content : contents) {
                        allContentIds.add(content.getId());
                        allContentsMap.put(content.getId(), content);
                    }
                }
            }
        }
        
        // Query all UserContentProgress for this enrollment at once with JOIN FETCH
        Map<Long, UserContentProgress> ucpMap = new HashMap<>();
        if (!allContentIds.isEmpty()) {
            ucpMap = ucpRepo
                    .findByEnrollment_IdAndContent_IdInWithContent(enrollment.getId(), allContentIds)
                    .stream()
                    .collect(Collectors.toMap(ucp -> ucp.getContent().getId(), ucp -> ucp));
        }
        
        // Build ContentProgressRes map from UserContentProgress and SectionsContent
        Map<Long, ContentProgressRes> contentProgressMap = new HashMap<>();
        for (Long contentId : allContentIds) {
            UserContentProgress ucp = ucpMap.get(contentId);
            SectionsContent content = allContentsMap.get(contentId);
            if (content != null) {
                contentProgressMap.put(contentId, ContentProgressRes.builder()
                        .contentId(contentId)
                        .contentFormat(content.getContentFormat())
                        .isTrackable(Boolean.TRUE.equals(content.getIsTrackable()))
                        .lastPositionSec(ucp != null ? ucp.getLastPositionSec() : null)
                        .isCompleted(ucp != null && Boolean.TRUE.equals(ucp.getIsCompleted()))
                        .durationSec(null)
                        .build());
            }
        }

        // Build tree structure
        for (Chapter chapter : chapters) {
            ChapterProgressRes chapterProgress = chapterProgressMap.get(chapter.getId());
            int chapterPercent = chapterProgress != null ? chapterProgress.getPercent() : 0;

            List<Lesson> lessons = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(chapter.getId());
            List<LessonLearningTreeRes> lessonTrees = new ArrayList<>(lessons.size());

            for (Lesson lesson : lessons) {
                LessonProgressRes lessonProgress = lessonProgressMap.get(lesson.getId());
                boolean isCompleted = lessonProgress != null && Boolean.TRUE.equals(lessonProgress.getIsCompleted());

                // Get quiz ID if exists
                Long quizId = quizRepo.findByLesson_Id(lesson.getId())
                        .map(Quiz::getId)
                        .orElse(null);

                List<Section> sections = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lesson.getId());
                List<SectionLearningTreeRes> sectionTrees = new ArrayList<>(sections.size());

                for (Section section : sections) {
                    List<SectionsContent> contents = contentRepo.findBySection_IdOrderByOrderIndexAsc(section.getId());
                    List<ContentLearningTreeRes> contentTrees = new ArrayList<>(contents.size());

                    for (SectionsContent content : contents) {
                        ContentProgressRes contentProgress = contentProgressMap.get(content.getId());
                        
                        contentTrees.add(ContentLearningTreeRes.builder()
                                .contentId(content.getId())
                                .orderIndex(content.getOrderIndex())
                                .contentFormat(content.getContentFormat())
                                .isPrimaryContent(content.isPrimaryContent())
                                .filePath(content.getFilePath())
                                .richText(content.getRichText())
                                .flashcardSetId(content.getFlashcardSetId())
                                .isTrackable(Boolean.TRUE.equals(content.getIsTrackable()))
                                .lastPositionSec(contentProgress != null ? contentProgress.getLastPositionSec() : null)
                                .isCompleted(contentProgress != null && Boolean.TRUE.equals(contentProgress.getIsCompleted()))
                                .durationSec(contentProgress != null ? contentProgress.getDurationSec() : null)
                                .build());
                    }

                    sectionTrees.add(SectionLearningTreeRes.builder()
                            .sectionId(section.getId())
                            .title(section.getTitle())
                            .orderIndex(section.getOrderIndex())
                            .studyType(section.getStudyType())
                            .flashcardSetId(section.getFlashcardSetId())
                            .contents(contentTrees)
                            .build());
                }

                lessonTrees.add(LessonLearningTreeRes.builder()
                        .lessonId(lesson.getId())
                        .title(lesson.getTitle())
                        .orderIndex(lesson.getOrderIndex())
                        .totalDurationSec(lesson.getTotalDurationSec())
                        .isCompleted(isCompleted)
                        .quizId(quizId)
                        .sections(sectionTrees)
                        .build());
            }

            chapterTrees.add(ChapterLearningTreeRes.builder()
                    .chapterId(chapter.getId())
                    .title(chapter.getTitle())
                    .orderIndex(chapter.getOrderIndex())
                    .summary(chapter.getSummary())
                    .progressPercent(chapterPercent)
                    .lessons(lessonTrees)
                    .build());
        }

        return CourseLearningTreeRes.builder()
                .courseId(courseId)
                .courseTitle(courseTitle)
                .courseSubtitle(courseSubtitle)
                .coverImagePath(coverImagePath)
                .enrollmentId(enrollment.getId())
                .progressPercent(enrollment.getProgressPercent())
                .lastAccessAt(enrollment.getLastAccessAt())
                .chapters(chapterTrees)
                .build();
    }

}
