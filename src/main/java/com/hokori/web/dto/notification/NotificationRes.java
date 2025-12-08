package com.hokori.web.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO cho notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRes {
    private Long id;
    private String type; // NotificationType enum name
    private String title;
    private String message;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;
    
    // Related entity IDs (để FE có thể navigate đến detail page)
    private Long relatedCourseId;
    private Long relatedPaymentId;
}

