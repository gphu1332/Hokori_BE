package com.hokori.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
// dto/TeacherApprovalSubmitRequest.java
public class TeacherApprovalSubmitRequest {
    @Size(max = 2000)
    private String message; // ghi chú gửi admin
}
