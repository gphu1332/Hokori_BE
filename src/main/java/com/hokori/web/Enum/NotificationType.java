package com.hokori.web.Enum;

/**
 * Enum định nghĩa các loại notification
 */
public enum NotificationType {
    // Teacher notifications
    COURSE_APPROVED,      // Course được approve (PUBLISHED)
    COURSE_REJECTED,      // Course bị reject
    COURSE_SUBMITTED,     // Course được submit để duyệt (PENDING_APPROVAL)
    COURSE_FLAGGED,       // Course bị flagged
    
    // Learner notifications
    PAYMENT_SUCCESS,      // Thanh toán thành công (course)
    AI_PACKAGE_ACTIVATED, // Gói AI đã được kích hoạt
    COURSE_ENROLLED,      // Đã enroll vào course (optional)
    COURSE_COMPLETED,     // Hoàn thành course (có certificate)
    
    // Teacher profile notifications
    PROFILE_APPROVED,     // Teacher profile được approve
    PROFILE_REJECTED,     // Teacher profile bị reject
    
    // System notifications
    SYSTEM_ANNOUNCEMENT   // Thông báo hệ thống
}

