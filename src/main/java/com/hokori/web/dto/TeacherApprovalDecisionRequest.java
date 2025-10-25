package com.hokori.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
// dto/TeacherApprovalDecisionRequest.java
public class TeacherApprovalDecisionRequest {
    @NotNull
    private Boolean approve; // true = duyệt, false = từ chối
    @Size(max = 2000)
    private String note;     // ghi chú của admin
}

