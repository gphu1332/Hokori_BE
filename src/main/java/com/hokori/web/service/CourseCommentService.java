package com.hokori.web.service;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.dto.comment.CommentCreateReq;
import com.hokori.web.dto.comment.CommentUpdateReq;
import com.hokori.web.dto.comment.CourseCommentDto;
import com.hokori.web.entity.Course;
import com.hokori.web.entity.CourseComment;
import com.hokori.web.entity.Enrollment;
import com.hokori.web.entity.User;
import com.hokori.web.repository.CourseCommentRepository;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCommentService {

    private final CourseCommentRepository commentRepo;
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final CurrentUserService currentUser;

    // ========= Helper =========

    private Course getCourseOrThrow(Long courseId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        // Nếu muốn chỉ cho comment trên course đã publish:
        if (c.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is not published");
        }
        if (c.isDeletedFlag()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        return c;
    }

    private void ensureLearnerEnrolled(Long userId, Long courseId) {
        boolean enrolled = enrollmentRepo.existsByUser_IdAndCourse_Id(userId, courseId);
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must enroll the course to comment");
        }
    }

    private boolean canModerateCourse(Long userId, Course course) {
        // Admin thì luôn được
        if (currentUser.isAdmin()) return true;
        // Giáo viên owner của course
        if (currentUser.isTeacher() && course.getUserId() != null) {
            return course.getUserId().equals(userId);
        }
        return false;
    }

    private CourseCommentDto toDtoWithReplies(CourseComment comment) {
        User u = comment.getUser();
        String authorName = (u.getDisplayName() != null && !u.getDisplayName().isBlank())
                ? u.getDisplayName()
                : u.getUsername();

        List<CourseCommentDto> replyDtos = comment.getReplies().stream()
                .filter(r -> !r.isDeletedFlag())
                .map(this::toDtoWithoutReplies)
                .toList();

        return new CourseCommentDto(
                comment.getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getCourse().getId(),
                u.getId(),
                authorName,
                u.getAvatarUrl(),
                comment.getContent(),
                comment.isEdited(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replyDtos
        );
    }

    private CourseCommentDto toDtoWithoutReplies(CourseComment comment) {
        User u = comment.getUser();
        String authorName = (u.getDisplayName() != null && !u.getDisplayName().isBlank())
                ? u.getDisplayName()
                : u.getUsername();

        return new CourseCommentDto(
                comment.getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getCourse().getId(),
                u.getId(),
                authorName,
                u.getAvatarUrl(),
                comment.getContent(),
                comment.isEdited(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                List.of()
        );
    }

    // ========= Public list =========

    @Transactional(readOnly = true)
    public Page<CourseCommentDto> listCourseComments(Long courseId, int page, int size) {
        getCourseOrThrow(courseId); // ensure course tồn tại + published

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CourseComment> roots =
                commentRepo.findByCourse_IdAndParentIsNullAndDeletedFlagFalseOrderByCreatedAtDesc(courseId, pageable);

        return roots.map(this::toDtoWithReplies);
    }

    // ========= Create =========

    public CourseCommentDto createRootComment(Long courseId, CommentCreateReq req) {
        Long userId = currentUser.getUserIdOrThrow();
        Course course = getCourseOrThrow(courseId);
        ensureLearnerEnrolled(userId, courseId);

        User user = userRepo.getReferenceById(userId);

        CourseComment c = new CourseComment();
        c.setCourse(course);
        c.setUser(user);
        c.setContent(req.content().trim());

        CourseComment saved = commentRepo.save(c);
        return toDtoWithReplies(saved);
    }

    public CourseCommentDto replyToComment(Long courseId, Long parentId, CommentCreateReq req) {
        Long userId = currentUser.getUserIdOrThrow();
        Course course = getCourseOrThrow(courseId);
        ensureLearnerEnrolled(userId, courseId);

        User user = userRepo.getReferenceById(userId);

        CourseComment parent = commentRepo.findByIdAndCourse_IdAndDeletedFlagFalse(parentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));

        CourseComment reply = new CourseComment();
        reply.setCourse(course);
        reply.setUser(user);
        reply.setParent(parent);
        reply.setContent(req.content().trim());

        CourseComment saved = commentRepo.save(reply);
        // Có thể trả về parent với list replies, hoặc chỉ reply
        return toDtoWithoutReplies(saved);
    }

    // ========= Update =========

    public CourseCommentDto updateComment(Long courseId, Long commentId, CommentUpdateReq req) {
        Long userId = currentUser.getUserIdOrThrow();
        CourseComment c = commentRepo.findByIdAndCourse_IdAndDeletedFlagFalse(commentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        Course course = c.getCourse();

        boolean isOwner = c.getUser().getId().equals(userId);
        boolean canModerate = canModerateCourse(userId, course);

        if (!isOwner && !canModerate) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to edit this comment");
        }

        c.setContent(req.content().trim());
        c.setEdited(true);

        CourseComment saved = commentRepo.save(c);
        return toDtoWithReplies(saved);
    }

    // ========= Delete (soft) =========

    public void deleteComment(Long courseId, Long commentId) {
        Long userId = currentUser.getUserIdOrThrow();
        CourseComment c = commentRepo.findByIdAndCourse_IdAndDeletedFlagFalse(commentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        Course course = c.getCourse();

        boolean isOwner = c.getUser().getId().equals(userId);
        boolean canModerate = canModerateCourse(userId, course);

        if (!isOwner && !canModerate) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to delete this comment");
        }

        c.setDeletedFlag(true); // soft delete
        commentRepo.save(c);
    }

    // ========= Teacher create root comment =========

    public CourseCommentDto createRootCommentAsTeacher(Long courseId, CommentCreateReq req) {
        Long userId = currentUser.getUserIdOrThrow();
        Course course = getCourseOrThrow(courseId);

        // chỉ teacher owner hoặc admin mới được
        if (!canModerateCourse(userId, course)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to comment as teacher");
        }

        User user = userRepo.getReferenceById(userId);

        CourseComment c = new CourseComment();
        c.setCourse(course);
        c.setUser(user);
        c.setContent(req.content().trim());

        CourseComment saved = commentRepo.save(c);
        return toDtoWithReplies(saved);
    }

    // ========= Teacher reply =========

    public CourseCommentDto replyToCommentAsTeacher(Long courseId, Long parentId, CommentCreateReq req) {
        Long userId = currentUser.getUserIdOrThrow();
        Course course = getCourseOrThrow(courseId);

        if (!canModerateCourse(userId, course)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to reply as teacher");
        }

        User user = userRepo.getReferenceById(userId);

        CourseComment parent = commentRepo.findByIdAndCourse_IdAndDeletedFlagFalse(parentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));

        CourseComment reply = new CourseComment();
        reply.setCourse(course);
        reply.setUser(user);
        reply.setParent(parent);
        reply.setContent(req.content().trim());

        CourseComment saved = commentRepo.save(reply);
        return toDtoWithoutReplies(saved);
    }

}
