package com.hokori.web.Enum;

public enum CourseStatus {
    DRAFT,
    PENDING_APPROVAL,
    REJECTED,      // moderator trả về
    PUBLISHED,
    FLAGGED,            // đã publish nhưng bị gắn cờ (tạm ẩn/giới hạn)
    ARCHIVED
}
