package com.hokori.web.Enum;

public enum WalletTransactionSource {
    COURSE_SALE,     // tiền giáo viên nhận từ bán khóa học
    TEACHER_PAYOUT,  // hệ thống trả tiền ra ngân hàng cho teacher
    ADMIN_ADJUST     // admin chỉnh tay (thưởng/phạt)
}