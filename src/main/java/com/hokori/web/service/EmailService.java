package com.hokori.web.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service để gửi email
 * Sử dụng JavaMailSender (Spring Boot starter mail)
 * 
 * Cấu hình trong application.properties:
 * - spring.mail.host=smtp.gmail.com
 * - spring.mail.port=587
 * - spring.mail.username=your-email@gmail.com
 * - spring.mail.password=your-app-password
 * - spring.mail.properties.mail.smtp.auth=true
 * - spring.mail.properties.mail.smtp.starttls.enable=true
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Gửi OTP qua email
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        log.info("EmailService.sendOtpEmail called - emailEnabled: {}, fromEmail: {}", emailEnabled, fromEmail);
        
        if (!emailEnabled) {
            log.warn("Email service is disabled. OTP for {}: {}", toEmail, otpCode);
            throw new RuntimeException("Email service is disabled");
        }

        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            log.warn("Email service is not configured (missing username). OTP for {}: {}", toEmail, otpCode);
            throw new RuntimeException("Email service is not configured (missing username)");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mã OTP đặt lại mật khẩu - Hokori");
            message.setText(buildOtpEmailContent(otpCode));

            log.info("Attempting to send OTP email to: {} from: {}", toEmail, fromEmail);
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (org.springframework.mail.MailException e) {
            log.error("Failed to send OTP email to {}: {}. OTP code: {}", 
                    toEmail, e.getMessage(), otpCode, e);
            
            // Check if it's a connection timeout issue
            Throwable cause = e.getCause();
            if (cause != null && (cause instanceof java.net.ConnectException || 
                                 cause.getMessage() != null && 
                                 cause.getMessage().contains("Connection timed out"))) {
                log.error("SMTP connection timeout. Please check: " +
                         "1) Network connectivity to smtp.gmail.com:587, " +
                         "2) Firewall rules allowing outbound SMTP, " +
                         "3) Gmail App Password is correct, " +
                         "4) SPRING_MAIL_HOST and SPRING_MAIL_PORT environment variables");
            }
            
            // Throw exception để PasswordResetService có thể handle và log đúng
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Unexpected error sending OTP email: " + e.getMessage(), e);
        }
    }

    /**
     * Build nội dung email OTP
     */
    private String buildOtpEmailContent(String otpCode) {
        return String.format("""
                Xin chào,
                
                Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Hokori của bạn.
                
                Mã OTP của bạn là: %s
                
                Mã này có hiệu lực trong 15 phút.
                
                Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                
                Trân trọng,
                Đội ngũ Hokori
                """, otpCode);
    }
}

