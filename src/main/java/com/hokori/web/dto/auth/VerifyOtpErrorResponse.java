package com.hokori.web.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để trả về thông tin về failed attempts khi verify OTP sai
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpErrorResponse {
    private String message;
    private Integer failedAttempts;
    private Integer remainingAttempts;
    private Integer maxAttempts;
}

