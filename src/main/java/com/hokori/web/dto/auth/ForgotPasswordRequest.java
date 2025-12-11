package com.hokori.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO để yêu cầu OTP qua email
 */
@Data
public class ForgotPasswordRequest {

    /**
     * Email của user để nhận OTP
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String emailOrPhone; // Giữ tên field để backward compatibility, nhưng chỉ nhận email
}

