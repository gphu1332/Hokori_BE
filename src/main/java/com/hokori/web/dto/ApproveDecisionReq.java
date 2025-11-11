package com.hokori.web.dto;

import com.hokori.web.Enum.ApprovalStatus;

// Admin quyết định
public record ApproveDecisionReq(
        ApprovalStatus action, // APPROVED hoặc REJECTED
        String note
) {}
