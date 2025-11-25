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

        Enrollment e = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled"));

        List<SectionsContent> contents = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lessonId).stream()
                .flatMap(s -> contentRepo.findBySection_IdOrderByOrderIndexAsc(s.getId()).stream())
                .collect(Collectors.toList());

        Map<Long, UserContentProgress> ucpMap = ucpRepo
                .findByEnrollment_IdAndContent_IdIn(e.getId(),
                        contents.stream().map(SectionsContent::getId).toList())
                .stream().collect(Collectors.toMap(c -> c.getContent().getId(), c -> c));

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
     */
    @Transactional(readOnly = true)
    public LessonRes getLessonDetail(Long userId, Long lessonId) {
        // Get courseId from lesson
        Long courseId = lessonRepo.findCourseIdByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        // Check enrollment
        Enrollment e = enrollmentRepo.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled in this course"));

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

        // Get flashcard set
        FlashcardSet set = flashcardSetRepo.findBySectionContent_IdAndDeletedFlagFalse(sectionContentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Flashcard set not found for this section content"));

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

}
