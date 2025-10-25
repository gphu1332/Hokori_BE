package com.hokori.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterTeacherRequest {
    @NotBlank @Size(min = 3, max = 100)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 100)
    private String password;

    //— Thông tin hồ sơ Udemy-style
    @NotBlank @Size(max = 100)
    private String firstName;

    @NotBlank @Size(max = 100)
    private String lastName;

    @NotBlank @Size(max = 60)
    private String headline;     // Đầu đề

    @NotBlank @Size(min = 50, max = 5000)
    private String bio;          // Tiểu sử (>=50 ký tự)

    // Mạng xã hội + website (optional)
    @Size(max = 255) private String websiteUrl;
    @Size(max = 255) private String facebook;
    @Size(max = 255) private String instagram;
    @Size(max = 255) private String linkedin;
    @Size(max = 255) private String tiktok;
    @Size(max = 255) private String x;
    @Size(max = 255) private String youtube;

    @NotBlank @Size(max = 50)
    private String language;     // Ngôn ngữ hiển thị

    // Yêu cầu chuyên môn
    @NotBlank
    private String currentJlptLevel; // Bắt buộc, và sẽ validate phải N2 hoặc N1
}
