package com.hokori.web.controller;

import com.hokori.web.dto.notification.NotificationRes;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "API để quản lý notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    /**
     * Lấy tất cả notifications của user hiện tại
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Lấy danh sách tất cả notifications", 
               description = "Lấy tất cả notifications của user hiện tại, sắp xếp theo thời gian (mới nhất trước)")
    public ResponseEntity<List<NotificationRes>> getMyNotifications() {
        Long userId = currentUserService.getUserIdOrThrow();
        List<NotificationRes> notifications = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Lấy notifications chưa đọc của user hiện tại
     */
    @GetMapping("/unread")
    @PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Lấy danh sách notifications chưa đọc", 
               description = "Lấy các notifications chưa đọc của user hiện tại")
    public ResponseEntity<List<NotificationRes>> getUnreadNotifications() {
        Long userId = currentUserService.getUserIdOrThrow();
        List<NotificationRes> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Đếm số notifications chưa đọc
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Đếm số notifications chưa đọc", 
               description = "Trả về số lượng notifications chưa đọc của user hiện tại")
    public ResponseEntity<Long> getUnreadCount() {
        Long userId = currentUserService.getUserIdOrThrow();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Đánh dấu một notification là đã đọc
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Đánh dấu notification là đã đọc", 
               description = "Đánh dấu một notification cụ thể là đã đọc")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        Long userId = currentUserService.getUserIdOrThrow();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Đánh dấu tất cả notifications của user là đã đọc
     */
    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Đánh dấu tất cả notifications là đã đọc", 
               description = "Đánh dấu tất cả notifications của user hiện tại là đã đọc")
    public ResponseEntity<Void> markAllAsRead() {
        Long userId = currentUserService.getUserIdOrThrow();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}

