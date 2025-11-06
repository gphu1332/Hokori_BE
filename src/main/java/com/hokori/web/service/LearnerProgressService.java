package com.hokori.web.service;

import com.hokori.web.dto.progress.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import lombok.RequiredArgsConstructor;
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
public class LearnerProgressService {

    private final EnrollmentRepository enrollmentRepo;
    private final ChapterRepository chapterRepo;
    private final LessonRepository lessonRepo;
    private final SectionRepository sectionRepo;
    private final SectionsContentRepository contentRepo;
    private final UserContentProgressRepository ucpRepo;

    // ================= Enrollment =================
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
}
