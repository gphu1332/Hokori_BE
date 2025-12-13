package com.hokori.web.Enum;

public enum CourseStatus {
    DRAFT,
    PENDING_APPROVAL,
    REJECTED,      // moderator trả về
    PUBLISHED,
    PENDING_UPDATE,    // đã publish nhưng có update đang chờ duyệt (course vẫn hiển thị với nội dung cũ)
    FLAGGED,            // đã publish nhưng bị gắn cờ (tạm ẩn/giới hạn)
    ARCHIVED
}
