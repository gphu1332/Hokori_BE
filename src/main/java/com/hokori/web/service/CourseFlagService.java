package com.hokori.web.service;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.FlagType;
import com.hokori.web.dto.course.CourseFlagReq;
import com.hokori.web.dto.course.FlaggedCourseRes;
import com.hokori.web.entity.Course;
import com.hokori.web.entity.CourseFlag;
import com.hokori.web.entity.User;
import com.hokori.web.repository.CourseFlagRepository;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseFlagService {

    private final CourseFlagRepository flagRepo;
    private final CourseRepository courseRepo;
    private final UserRepository userRepo;

    /**
     * User flag một course
     */
    public void flagCourse(Long courseId, Long userId, CourseFlagReq req) {
        Course course = courseRepo.findByIdAndDeletedFlagFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Chỉ cho phép flag course đã PUBLISHED
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only published courses can be flagged");
        }

        // Kiểm tra user đã flag course này chưa
        if (flagRepo.existsByCourse_IdAndUserId(courseId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already flagged this course");
        }

        // Tạo flag
        CourseFlag flag = new CourseFlag();
        flag.setCourse(course);
        flag.setUserId(userId);
        flag.setFlagType(req.getFlagType());
        flag.setReason(req.getReason());
        flag.setCreatedAt(Instant.now());

        flagRepo.save(flag);
        log.info("Course {} flagged by user {} with type {}", courseId, userId, req.getFlagType());
    }

    /**
     * Moderator xem danh sách courses bị flag (sắp xếp theo số lượng flag)
     */
    @Transactional(readOnly = true)
    public List<FlaggedCourseRes> listFlaggedCourses() {
        List<Object[]> flaggedCourses = flagRepo.findFlaggedCoursesWithDetails();
        
        List<FlaggedCourseRes> result = new ArrayList<>();
        
        for (Object[] row : flaggedCourses) {
            Long courseId = ((Number) row[0]).longValue();
            String courseTitle = row[1] != null ? row[1].toString() : "";
            String courseSlug = row[2] != null ? row[2].toString() : "";
            Long teacherId = row[3] != null ? ((Number) row[3]).longValue() : null;
            Long flagCount = row[5] != null ? ((Number) row[5]).longValue() : 0L;
            Instant latestFlagAt = row[6] != null ? (Instant) row[6] : null;

            // Lấy course để lấy thông tin flag
            Course course = courseRepo.findById(courseId).orElse(null);
            String flaggedReason = course != null ? course.getFlaggedReason() : null;
            Instant flaggedAt = course != null ? course.getFlaggedAt() : null;
            Long flaggedByUserId = course != null ? course.getFlaggedByUserId() : null;

            // Lấy teacher name
            String teacherName = null;
            if (teacherId != null) {
                User teacher = userRepo.findById(teacherId).orElse(null);
                if (teacher != null) {
                    teacherName = teacher.getDisplayName() != null && !teacher.getDisplayName().isEmpty()
                            ? teacher.getDisplayName()
                            : teacher.getUsername();
                }
            }

            // Lấy moderator name
            String flaggedByUserName = null;
            if (flaggedByUserId != null) {
                User moderator = userRepo.findById(flaggedByUserId).orElse(null);
                if (moderator != null) {
                    flaggedByUserName = moderator.getDisplayName() != null && !moderator.getDisplayName().isEmpty()
                            ? moderator.getDisplayName()
                            : moderator.getUsername();
                }
            }

            // Lấy danh sách flags chi tiết
            List<CourseFlag> flags = flagRepo.findByCourse_IdOrderByCreatedAtDesc(courseId);
            List<FlaggedCourseRes.FlagDetailRes> flagDetails = flags.stream()
                    .map(flag -> {
                        User flagUser = userRepo.findById(flag.getUserId()).orElse(null);
                        String userName = "Unknown";
                        if (flagUser != null) {
                            userName = flagUser.getDisplayName() != null && !flagUser.getDisplayName().isEmpty()
                                    ? flagUser.getDisplayName()
                                    : flagUser.getUsername();
                        }
                        
                        return FlaggedCourseRes.FlagDetailRes.builder()
                                .flagId(flag.getId())
                                .flagType(flag.getFlagType().name())
                                .reason(flag.getReason())
                                .userId(flag.getUserId())
                                .userName(userName)
                                .createdAt(flag.getCreatedAt())
                                .build();
                    })
                    .collect(Collectors.toList());

            FlaggedCourseRes flaggedCourse = FlaggedCourseRes.builder()
                    .courseId(courseId)
                    .courseTitle(courseTitle)
                    .courseSlug(courseSlug)
                    .teacherId(teacherId)
                    .teacherName(teacherName)
                    .flagCount(flagCount)
                    .latestFlagAt(latestFlagAt)
                    .flaggedReason(flaggedReason)
                    .flaggedAt(flaggedAt)
                    .flaggedByUserId(flaggedByUserId)
                    .flaggedByUserName(flaggedByUserName)
                    .flags(flagDetails)
                    .build();

            result.add(flaggedCourse);
        }

        return result;
    }

    /**
     * Moderator flag course (set status = FLAGGED và ẩn course)
     */
    public void moderatorFlagCourse(Long courseId, Long moderatorUserId) {
        Course course = courseRepo.findByIdAndDeletedFlagFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Chỉ cho phép flag course đang PUBLISHED
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only published courses can be flagged by moderator");
        }

        // Tổng hợp lý do từ các flags
        List<CourseFlag> flags = flagRepo.findByCourse_IdOrderByCreatedAtDesc(courseId);
        StringBuilder reasonBuilder = new StringBuilder();
        reasonBuilder.append("Course flagged due to multiple user reports:\n");
        for (CourseFlag flag : flags) {
            reasonBuilder.append(String.format("- %s: %s\n", 
                    flag.getFlagType().name(), 
                    flag.getReason() != null ? flag.getReason() : "No reason provided"));
        }

        // Set status = FLAGGED và lưu thông tin
        course.setStatus(CourseStatus.FLAGGED);
        course.setFlaggedReason(reasonBuilder.toString());
        course.setFlaggedAt(Instant.now());
        course.setFlaggedByUserId(moderatorUserId);

        courseRepo.save(course);
        log.info("Course {} flagged by moderator {}", courseId, moderatorUserId);
    }

    /**
     * Teacher xem lý do flag
     */
    @Transactional(readOnly = true)
    public FlaggedCourseRes getFlagReason(Long courseId, Long teacherUserId) {
        Course course = courseRepo.findByIdAndDeletedFlagFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Chỉ owner mới xem được
        if (!course.getUserId().equals(teacherUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this course");
        }

        // Chỉ course bị FLAGGED mới có lý do
        if (course.getStatus() != CourseStatus.FLAGGED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Course is not flagged");
        }

        // Lấy thông tin flags
        List<CourseFlag> flags = flagRepo.findByCourse_IdOrderByCreatedAtDesc(courseId);
        List<FlaggedCourseRes.FlagDetailRes> flagDetails = flags.stream()
                .map(flag -> {
                    User flagUser = userRepo.findById(flag.getUserId()).orElse(null);
                    String userName = "Unknown";
                    if (flagUser != null) {
                        userName = flagUser.getDisplayName() != null && !flagUser.getDisplayName().isEmpty()
                                ? flagUser.getDisplayName()
                                : flagUser.getUsername();
                    }
                    
                    return FlaggedCourseRes.FlagDetailRes.builder()
                            .flagId(flag.getId())
                            .flagType(flag.getFlagType().name())
                            .reason(flag.getReason())
                            .userId(flag.getUserId())
                            .userName(userName)
                            .createdAt(flag.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        // Lấy moderator name
        String flaggedByUserName = null;
        if (course.getFlaggedByUserId() != null) {
            User moderator = userRepo.findById(course.getFlaggedByUserId()).orElse(null);
            if (moderator != null) {
                flaggedByUserName = moderator.getDisplayName() != null && !moderator.getDisplayName().isEmpty()
                        ? moderator.getDisplayName()
                        : moderator.getUsername();
            }
        }

        return FlaggedCourseRes.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .courseSlug(course.getSlug())
                .teacherId(course.getUserId())
                .flagCount((long) flags.size())
                .latestFlagAt(flags.isEmpty() ? null : flags.get(0).getCreatedAt())
                .flaggedReason(course.getFlaggedReason())
                .flaggedAt(course.getFlaggedAt())
                .flaggedByUserId(course.getFlaggedByUserId())
                .flaggedByUserName(flaggedByUserName)
                .flags(flagDetails)
                .build();
    }

    /**
     * Teacher resubmit course sau khi sửa
     */
    public void resubmitCourse(Long courseId, Long teacherUserId) {
        Course course = courseRepo.findByIdAndDeletedFlagFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Chỉ owner mới resubmit được
        if (!course.getUserId().equals(teacherUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this course");
        }

        // Chỉ course bị FLAGGED mới resubmit được
        if (course.getStatus() != CourseStatus.FLAGGED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only flagged courses can be resubmitted");
        }

        // Chuyển về PENDING_APPROVAL để moderator review lại
        course.setStatus(CourseStatus.PENDING_APPROVAL);
        // Giữ lại flaggedReason để teacher biết lý do, nhưng clear flaggedAt và flaggedByUserId
        // (có thể giữ lại để trace history)
        // course.setFlaggedReason(null);
        // course.setFlaggedAt(null);
        // course.setFlaggedByUserId(null);

        courseRepo.save(course);
        log.info("Course {} resubmitted by teacher {}", courseId, teacherUserId);
    }
}

