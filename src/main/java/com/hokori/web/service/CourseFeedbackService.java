// com.hokori.web.service.CourseFeedbackService.java
package com.hokori.web.service;

import com.hokori.web.dto.course.CourseFeedbackReq;
import com.hokori.web.dto.course.CourseFeedbackRes;
import com.hokori.web.dto.course.CourseFeedbackSummaryRes;
import com.hokori.web.entity.Course;
import com.hokori.web.entity.CourseFeedback;
import com.hokori.web.entity.User;
import com.hokori.web.repository.CourseFeedbackRepository;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseFeedbackService {

    private final CourseFeedbackRepository feedbackRepo;
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;

    // ====== CREATE / UPDATE (upsert) feedback cho 1 course ======
    public CourseFeedbackRes upsertFeedback(Long userId, Long courseId, CourseFeedbackReq req) {
        // 1. Check course tồn tại
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // 2. Check deletedFlag nếu có
        try {
            // nếu có field deletedFlag thì check
            var f = Course.class.getDeclaredField("deletedFlag");
            f.setAccessible(true);
            Object val = f.get(course);
            if (val instanceof Boolean b && Boolean.TRUE.equals(b)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // không có field -> bỏ qua
        }

        // 3. Chỉ learner đã enroll mới được feedback
        if (!enrollmentRepo.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You must enroll in this course before giving feedback"
            );
        }

        // 4. Lấy user
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        // 5. Tìm feedback cũ (nếu có) -> upsert
        CourseFeedback feedback = feedbackRepo
                .findByCourse_IdAndUser_IdAndDeletedFlagFalse(courseId, userId)
                .orElseGet(() -> CourseFeedback.builder()
                        .course(course)
                        .user(user)
                        .build());

        feedback.setRating(req.getRating());
        feedback.setComment(req.getComment());
        feedback.setDeletedFlag(false);

        feedback = feedbackRepo.save(feedback);

        // 6. Recompute ratingAvg + ratingCount cho course (UPDATE bảng course)
        recomputeCourseRating(courseId);

        return toRes(feedback);
    }

    // ====== Xoá (soft delete) feedback ======
    public void deleteFeedback(Long userId, Long feedbackId) {
        CourseFeedback feedback = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found"));

        if (!feedback.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner of this feedback");
        }

        feedback.setDeletedFlag(true);
        feedbackRepo.save(feedback);

        // cập nhật lại rating cho course sau khi xoá
        recomputeCourseRating(feedback.getCourse().getId());
    }

    // ====== Lấy list feedback public cho course detail ======
    @Transactional(readOnly = true)
    public List<CourseFeedbackRes> listFeedbacksForCourse(Long courseId) {
        List<CourseFeedback> list =
                feedbackRepo.findByCourse_IdAndDeletedFlagFalseOrderByCreatedAtDesc(courseId);
        return list.stream().map(this::toRes).toList();
    }

    // ====== Lấy summary (avg + count) cho FE hiển thị sao ======
    @Transactional(readOnly = true)
    public CourseFeedbackSummaryRes getSummary(Long courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Double avg = course.getRatingAvg();
        Long cnt   = course.getRatingCount();

        double ratingAvg   = (avg == null) ? 0.0 : avg;
        long   ratingCount = (cnt == null) ? 0L  : cnt;

        return new CourseFeedbackSummaryRes(ratingAvg, ratingCount);
    }

    // ====== Helper: map entity -> DTO ======
    private CourseFeedbackRes toRes(CourseFeedback f) {
        User u = f.getUser();
        String name = null;

        try {
            var m = User.class.getMethod("getDisplayName");
            Object v = m.invoke(u);
            if (v != null) name = v.toString();
        } catch (Exception ignored) {
        }

        if (name == null) {
            // fallback: first + last name hoặc username
            try {
                var mFirst = User.class.getMethod("getFirstName");
                var mLast = User.class.getMethod("getLastName");
                Object fName = mFirst.invoke(u);
                Object lName = mLast.invoke(u);
                if (fName != null || lName != null) {
                    name = (fName == null ? "" : fName.toString()) + " " +
                            (lName == null ? "" : lName.toString());
                }
            } catch (Exception ignored) {
            }
            if (name == null || name.isBlank()) {
                try {
                    var mUser = User.class.getMethod("getUsername");
                    Object v = mUser.invoke(u);
                    if (v != null) name = v.toString();
                } catch (Exception ignored) {
                    name = "Learner";
                }
            }
        }

        String avatar = null;
        try {
            var m = User.class.getMethod("getAvatarUrl");
            Object v = m.invoke(u);
            if (v != null) avatar = v.toString();
        } catch (Exception ignored) {
        }

        return CourseFeedbackRes.builder()
                .id(f.getId())
                .userId(u.getId())
                .learnerName(name)
                .learnerAvatarUrl(avatar)
                .rating(f.getRating())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .build();
    }

    // ====== Helper: tính AVG/COUNT và UPDATE bảng course ======
    private void recomputeCourseRating(Long courseId) {
        // 1) Lấy toàn bộ feedback còn sống (deletedFlag = false)
        List<CourseFeedback> feedbacks =
                feedbackRepo.findByCourse_IdAndDeletedFlagFalseOrderByCreatedAtDesc(courseId);

        // 2) Tính count + avg ngay trong Java
        long count = feedbacks.size();
        double avg  = 0.0;

        if (count > 0) {
            avg = feedbacks.stream()
                    .mapToInt(f -> {
                        Integer r = f.getRating();
                        return (r == null) ? 0 : r;
                    })
                    .average()
                    .orElse(0.0);
        }

        // 3) Load lại course, set 2 field rồi save
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        course.setRatingAvg(avg);          // Double
        course.setRatingCount(count);      // Long (auto boxing)

        // đang ở trong @Transactional nên chỉ cần dirty checking cũng được,
        // nhưng gọi save cho chắc chắn sinh UPDATE
        courseRepo.save(course);
    }


}
