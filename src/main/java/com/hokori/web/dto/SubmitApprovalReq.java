package com.hokori.web.dto;

import java.util.List;

// Teacher gọi khi nộp: nếu bỏ trống -> mặc định nộp tất cả certificates của user
public record SubmitApprovalReq(
        String note,
        List<Long> certificateIds
) {}

