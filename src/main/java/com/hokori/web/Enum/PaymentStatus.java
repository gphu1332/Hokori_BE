package com.hokori.web.Enum;

public enum PaymentStatus {
    PENDING,      // Đang chờ thanh toán
    PAID,         // Đã thanh toán thành công
    CANCELLED,    // Đã hủy
    FAILED,       // Thanh toán thất bại
    EXPIRED       // Hết hạn thanh toán
}

