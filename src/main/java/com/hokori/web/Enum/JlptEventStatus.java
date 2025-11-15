package com.hokori.web.Enum;

public enum JlptEventStatus {
    DRAFT,      // Admin tạo xong nhưng chưa mở
    OPEN,       // Đang mở cho user thi
    CLOSED,     // Đã đóng, không cho làm mới
    FINISHED    // Đã chấm/xử lý xong (optional)
}
