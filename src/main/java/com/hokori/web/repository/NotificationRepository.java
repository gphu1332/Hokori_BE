package com.hokori.web.repository;

import com.hokori.web.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy tất cả notifications của một user, sắp xếp theo thời gian tạo (mới nhất trước)
     */
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * Lấy notifications chưa đọc của một user
     */
    List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Đếm số notifications chưa đọc của một user
     */
    long countByUser_IdAndIsReadFalse(Long userId);

    /**
     * Đánh dấu notification là đã đọc
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.user.id = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Đánh dấu tất cả notifications của user là đã đọc
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);
}

