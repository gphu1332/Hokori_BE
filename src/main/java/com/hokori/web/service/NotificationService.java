package com.hokori.web.service;

import com.hokori.web.Enum.NotificationType;
import com.hokori.web.dto.notification.NotificationRes;
import com.hokori.web.entity.Notification;
import com.hokori.web.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepo;

    /**
     * Tạo notification cho user
     */
    public Notification createNotification(Long userId, NotificationType type, String title, String message, 
                                          Long relatedCourseId, Long relatedPaymentId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setRelatedCourseId(relatedCourseId);
        notification.setRelatedPaymentId(relatedPaymentId);
        notification.setCreatedAt(Instant.now());

        Notification saved = notificationRepo.save(notification);
        log.info("Notification created: id={}, userId={}, type={}, title={}", 
                saved.getId(), userId, type, title);
        return saved;
    }

    /**
     * Tạo notification cho teacher khi course được approve
     */
    public void notifyCourseApproved(Long teacherId, Long courseId, String courseTitle) {
        createNotification(
                teacherId,
                NotificationType.COURSE_APPROVED,
                "Khóa học đã được duyệt",
                String.format("Khóa học \"%s\" của bạn đã được duyệt và đã được publish.", courseTitle),
                courseId,
                null
        );
    }

    /**
     * Tạo notification cho teacher khi course bị reject
     */
    public void notifyCourseRejected(Long teacherId, Long courseId, String courseTitle, String reason) {
        String message = String.format("Khóa học \"%s\" của bạn đã bị từ chối.", courseTitle);
        if (reason != null && !reason.isBlank()) {
            message += "\nLý do: " + reason;
        }
        
        createNotification(
                teacherId,
                NotificationType.COURSE_REJECTED,
                "Khóa học bị từ chối",
                message,
                courseId,
                null
        );
    }

    /**
     * Tạo notification cho teacher khi course được submit để duyệt
     */
    public void notifyCourseSubmitted(Long teacherId, Long courseId, String courseTitle) {
        createNotification(
                teacherId,
                NotificationType.COURSE_SUBMITTED,
                "Khóa học đã được gửi để duyệt",
                String.format("Khóa học \"%s\" của bạn đã được gửi để moderator duyệt.", courseTitle),
                courseId,
                null
        );
    }

    /**
     * Tạo notification cho teacher khi course bị flagged
     */
    public void notifyCourseFlagged(Long teacherId, Long courseId, String courseTitle, String reason) {
        String message = String.format("Khóa học \"%s\" của bạn đã bị flagged và tạm thời bị ẩn khỏi public.", courseTitle);
        if (reason != null && !reason.isBlank()) {
            message += "\nLý do: " + reason;
        }
        
        createNotification(
                teacherId,
                NotificationType.COURSE_FLAGGED,
                "Khóa học bị flagged",
                message,
                courseId,
                null
        );
    }

    /**
     * Tạo notification cho learner khi thanh toán thành công
     */
    public void notifyPaymentSuccess(Long userId, Long paymentId, Long courseId, String courseTitle) {
        createNotification(
                userId,
                NotificationType.PAYMENT_SUCCESS,
                "Thanh toán thành công",
                String.format("Bạn đã thanh toán thành công cho khóa học \"%s\". Bạn có thể bắt đầu học ngay!", courseTitle),
                courseId,
                paymentId
        );
    }

    /**
     * Tạo notification cho learner khi hoàn thành course
     */
    public void notifyCourseCompleted(Long userId, Long courseId, String courseTitle) {
        createNotification(
                userId,
                NotificationType.COURSE_COMPLETED,
                "Chúc mừng! Bạn đã hoàn thành khóa học",
                String.format("Bạn đã hoàn thành 100%% khóa học \"%s\". Hãy xem certificate của bạn!", courseTitle),
                courseId,
                null
        );
    }

    /**
     * Tạo notification cho teacher khi profile được approve
     */
    public void notifyProfileApproved(Long teacherId, String reason) {
        String message = "Hồ sơ giáo viên của bạn đã được duyệt. Bạn có thể bắt đầu tạo và publish khóa học!";
        if (reason != null && !reason.isBlank()) {
            message += "\nGhi chú: " + reason;
        }
        
        createNotification(
                teacherId,
                NotificationType.PROFILE_APPROVED,
                "Hồ sơ giáo viên đã được duyệt",
                message,
                null,
                null
        );
    }

    /**
     * Tạo notification cho teacher khi profile bị reject
     */
    public void notifyProfileRejected(Long teacherId, String reason) {
        String message = "Hồ sơ giáo viên của bạn đã bị từ chối.";
        if (reason != null && !reason.isBlank()) {
            message += "\nLý do: " + reason;
        }
        message += "\nBạn có thể chỉnh sửa và gửi lại để được duyệt.";
        
        createNotification(
                teacherId,
                NotificationType.PROFILE_REJECTED,
                "Hồ sơ giáo viên bị từ chối",
                message,
                null,
                null
        );
    }

    /**
     * Tạo notification cho learner khi AI package được kích hoạt
     */
    public void notifyAIPackageActivated(Long userId, Long packageId, String packageName) {
        createNotification(
                userId,
                NotificationType.AI_PACKAGE_ACTIVATED,
                "Gói AI đã được kích hoạt",
                String.format("Gói AI \"%s\" của bạn đã được kích hoạt thành công. Bạn có thể sử dụng các tính năng AI ngay!", packageName),
                null,
                null
        );
    }

    /**
     * Lấy tất cả notifications của user
     */
    @Transactional(readOnly = true)
    public List<NotificationRes> getMyNotifications(Long userId) {
        List<Notification> notifications = notificationRepo.findByUser_IdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    /**
     * Lấy notifications chưa đọc của user
     */
    @Transactional(readOnly = true)
    public List<NotificationRes> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepo.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    /**
     * Đếm số notifications chưa đọc
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUser_IdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu notification là đã đọc
     */
    public void markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepo.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Notification not found or you don't have permission");
        }
        log.info("Notification marked as read: notificationId={}, userId={}", notificationId, userId);
    }

    /**
     * Đánh dấu tất cả notifications của user là đã đọc
     */
    public void markAllAsRead(Long userId) {
        int updated = notificationRepo.markAllAsRead(userId);
        log.info("Marked {} notifications as read for userId={}", updated, userId);
    }

    /**
     * Map entity to DTO
     */
    private NotificationRes toRes(Notification notification) {
        return NotificationRes.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .relatedCourseId(notification.getRelatedCourseId())
                .relatedPaymentId(notification.getRelatedPaymentId())
                .build();
    }
}

