package com.hokori.web.service;

import com.hokori.web.entity.PasswordResetOtp;
import com.hokori.web.entity.User;
import com.hokori.web.repository.PasswordResetOtpRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service để quản lý password reset với OTP
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_LENGTH = 6;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Tạo và gửi OTP qua email
     */
    public void requestOtpByEmail(String email) {
        // Kiểm tra user tồn tại
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Không tiết lộ user không tồn tại (security best practice)
            log.warn("Password reset requested for non-existent email: {}", email);
            return; // Return success để không tiết lộ thông tin
        }

        User user = userOpt.get();
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        // Tạo OTP
        String otpCode = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // Lưu OTP vào database
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setExpiresAt(expiresAt);
        otp.setIsUsed(false);
        otp.setFailedAttempts(0);
        otpRepository.save(otp);

        // Gửi email
        emailService.sendOtpEmail(email, otpCode);

        log.info("OTP requested for email: {}", email);
    }

    /**
     * Tạo và gửi OTP qua SMS
     */
    public void requestOtpByPhone(String phoneNumber) {
        // Kiểm tra user tồn tại
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
        if (userOpt.isEmpty()) {
            // Không tiết lộ user không tồn tại (security best practice)
            log.warn("Password reset requested for non-existent phone: {}", phoneNumber);
            return; // Return success để không tiết lộ thông tin
        }

        User user = userOpt.get();
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        // Tạo OTP
        String otpCode = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // Lưu OTP vào database
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setPhoneNumber(phoneNumber);
        otp.setOtpCode(otpCode);
        otp.setExpiresAt(expiresAt);
        otp.setIsUsed(false);
        otp.setFailedAttempts(0);
        otpRepository.save(otp);

        // Gửi SMS
        smsService.sendOtpSms(phoneNumber, otpCode);

        log.info("OTP requested for phone: {}", phoneNumber);
    }

    /**
     * Verify OTP và trả về token để reset password
     */
    public String verifyOtp(String emailOrPhone, String otpCode) {
        LocalDateTime now = LocalDateTime.now();

        // Tìm OTP hợp lệ
        Optional<PasswordResetOtp> otpOpt;
        if (emailOrPhone.contains("@")) {
            // Email
            otpOpt = otpRepository.findLatestValidByEmail(emailOrPhone, now);
        } else {
            // Phone number
            otpOpt = otpRepository.findLatestValidByPhoneNumber(emailOrPhone, now);
        }

        if (otpOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        PasswordResetOtp otp = otpOpt.get();

        // Kiểm tra số lần verify sai
        if (otp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has been locked due to too many failed attempts");
        }

        // Verify OTP code
        if (!otp.getOtpCode().equals(otpCode)) {
            otpRepository.incrementFailedAttempts(otp.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP code");
        }

        // Đánh dấu OTP đã sử dụng
        otpRepository.markAsUsed(otp.getId());

        // Tạo reset token (có thể dùng JWT hoặc random string)
        // Đơn giản: return email/phone để dùng trong reset password
        return emailOrPhone;
    }

    /**
     * Reset password sau khi verify OTP thành công
     * Cần verify OTP trước khi gọi method này
     */
    public void resetPassword(String emailOrPhone, String otpCode, String newPassword) {
        // Verify OTP trước khi reset password
        verifyOtp(emailOrPhone, otpCode);
        
        // Tìm user
        Optional<User> userOpt;
        if (emailOrPhone.contains("@")) {
            userOpt = userRepository.findByEmail(emailOrPhone);
        } else {
            userOpt = userRepository.findByPhoneNumber(emailOrPhone);
        }

        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        User user = userOpt.get();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successful for: {}", emailOrPhone);
    }

    /**
     * Generate 6-digit OTP code
     */
    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // 100000-999999
        return String.valueOf(otp);
    }
}

