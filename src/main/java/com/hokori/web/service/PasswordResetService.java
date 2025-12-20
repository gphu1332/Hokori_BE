package com.hokori.web.service;

import com.hokori.web.entity.PasswordResetLockout;
import com.hokori.web.entity.PasswordResetOtp;
import com.hokori.web.entity.User;
import com.hokori.web.repository.PasswordResetLockoutRepository;
import com.hokori.web.repository.PasswordResetOtpRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hokori.web.exception.InvalidOtpException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service để quản lý password reset với OTP
 * Có brute-force protection: khóa chức năng forgot password khi nhập sai quá 5 lần
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordResetLockoutRepository lockoutRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    
    @PersistenceContext
    private EntityManager entityManager;

    private static final int OTP_LENGTH = 6;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30; // Khóa trong 30 phút
    private static final SecureRandom random = new SecureRandom();

    /**
     * Tạo và gửi OTP qua email (backward compatibility - không có IP)
     */
    public void requestOtpByEmail(String email) {
        requestOtpByEmail(email, null);
    }
    
    /**
     * Tạo và gửi OTP qua email
     * 
     * @param email Email của user
     * @param ipAddress IP address của client (nullable)
     */
    public void requestOtpByEmail(String email, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        
        // Kiểm tra lockout cho email hoặc IP
        Optional<PasswordResetLockout> lockoutOpt = lockoutRepository.findActiveLockoutByEmailOrIp(email, ipAddress, now);
        if (lockoutOpt.isPresent()) {
            PasswordResetLockout lockout = lockoutOpt.get();
            long minutesRemaining = java.time.Duration.between(now, lockout.getUnlockAt()).toMinutes();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                    String.format("Password reset function is temporarily locked due to too many failed attempts. Please try again in %d minutes.", 
                            minutesRemaining + 1));
        }
        
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
        LocalDateTime expiresAt = now.plusMinutes(15);

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

        log.info("OTP requested for email: {}, IP: {}", email, ipAddress);
    }

    /**
     * Verify OTP và trả về token để reset password (backward compatibility - không có IP)
     */
    public String verifyOtp(String email, String otpCode) {
        return verifyOtp(email, otpCode, null);
    }
    
    /**
     * Verify OTP và trả về token để reset password
     * OTP sẽ được mark as used sau khi verify thành công
     * 
     * @param email Email của user
     * @param otpCode Mã OTP
     * @param ipAddress IP address của client (nullable)
     */
    public String verifyOtp(String email, String otpCode, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        
        // Kiểm tra lockout cho email hoặc IP
        Optional<PasswordResetLockout> lockoutOpt = lockoutRepository.findActiveLockoutByEmailOrIp(email, ipAddress, now);
        if (lockoutOpt.isPresent()) {
            PasswordResetLockout lockout = lockoutOpt.get();
            long minutesRemaining = java.time.Duration.between(now, lockout.getUnlockAt()).toMinutes();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                    String.format("Password reset function is temporarily locked due to too many failed attempts. Please try again in %d minutes.", 
                            minutesRemaining + 1));
        }

        // Tìm OTP hợp lệ theo email
        Optional<PasswordResetOtp> otpOpt = otpRepository.findLatestValidByEmail(email, now);

        if (otpOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        PasswordResetOtp otp = otpOpt.get();

        // Kiểm tra số lần verify sai
        if (otp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            // Tạo lockout khi đạt max failed attempts
            createLockout(email, ipAddress, "Too many failed OTP attempts");
            long minutesRemaining = LOCKOUT_DURATION_MINUTES;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                    String.format("OTP has been locked due to too many failed attempts. Password reset function is temporarily locked for %d minutes.", 
                            minutesRemaining));
        }

        // Verify OTP code
        if (!otp.getOtpCode().equals(otpCode)) {
            otpRepository.incrementFailedAttempts(otp.getId());
            
            // Kiểm tra lại sau khi increment - nếu đạt max thì tạo lockout
            PasswordResetOtp updatedOtp = otpRepository.findById(otp.getId()).orElse(otp);
            int newFailedAttempts = updatedOtp.getFailedAttempts();
            
            if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
                createLockout(email, ipAddress, "Too many failed OTP attempts");
                long minutesRemaining = LOCKOUT_DURATION_MINUTES;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                        String.format("Too many failed attempts. Password reset function is temporarily locked for %d minutes.", 
                                minutesRemaining));
            }
            
            // Throw exception với thông tin về failed attempts
            throw new InvalidOtpException(newFailedAttempts, MAX_FAILED_ATTEMPTS);
        }

        // Đánh dấu OTP đã sử dụng (đã verify)
        otpRepository.markAsUsed(otp.getId());

        // Return email để dùng trong reset password
        return email;
    }
    
    /**
     * Tạo lockout cho email/IP khi brute-force attack
     * Sử dụng REQUIRES_NEW để đảm bảo lockout được commit ngay lập tức,
     * không bị rollback khi throw exception trong transaction chính
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void createLockout(String email, String ipAddress, String reason) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unlockAt = now.plusMinutes(LOCKOUT_DURATION_MINUTES);
        
        PasswordResetLockout lockout = new PasswordResetLockout();
        lockout.setEmail(email);
        lockout.setIpAddress(ipAddress);
        lockout.setLockedAt(now);
        lockout.setUnlockAt(unlockAt);
        lockout.setReason(reason);
        lockout.setIsUnlocked(false);
        
        lockoutRepository.save(lockout);
        entityManager.flush(); // Force flush để đảm bảo commit ngay
        
        log.warn("Password reset lockout created for email: {}, IP: {}, reason: {}, unlock at: {}", 
                email, ipAddress, reason, unlockAt);
    }

    /**
     * Reset password sau khi verify OTP thành công
     * Cần verify OTP trước khi gọi method này (qua API /forgot-password/verify-otp)
     * 
     * Logic: Tìm OTP đã được verify (isUsed = true) và chưa hết hạn
     * Không verify lại OTP vì đã verify ở bước trước đó
     */
    public void resetPassword(String email, String otpCode, String newPassword) {
        LocalDateTime now = LocalDateTime.now();
        
        log.debug("Attempting to reset password for email: {}, OTP code: {}, Current time: {}", email, otpCode, now);
        
        // Tìm OTP đã được verify (isUsed = true)
        // OTP đã được verify ở bước /forgot-password/verify-otp trước đó
        // Không check expiresAt vì OTP đã được verify, cho phép reset password trong một khoảng thời gian hợp lý
        Optional<PasswordResetOtp> otpOpt = otpRepository.findVerifiedOtpByEmailAndCode(email, otpCode);
        
        if (otpOpt.isEmpty()) {
            log.warn("Verified OTP not found for email: {}, OTP code: {}, Current time: {}", email, otpCode, now);
            
            // Nếu không tìm thấy OTP đã verify, có thể OTP chưa được verify hoặc đã hết hạn
            // Fallback: Tìm OTP chưa verify để verify lại (trường hợp user skip bước verify-otp)
            Optional<PasswordResetOtp> unverifiedOtpOpt = otpRepository.findLatestValidByEmail(email, now);
            if (unverifiedOtpOpt.isPresent()) {
                PasswordResetOtp otp = unverifiedOtpOpt.get();
                log.debug("Found unverified OTP, verifying now. OTP expires at: {}", otp.getExpiresAt());
                
                // Verify OTP code
                if (!otp.getOtpCode().equals(otpCode)) {
                    otpRepository.incrementFailedAttempts(otp.getId());
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP code");
                }
                // Kiểm tra số lần verify sai
                if (otp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has been locked due to too many failed attempts");
                }
                // Mark as used
                otpRepository.markAsUsed(otp.getId());
            } else {
                log.error("No valid OTP found (neither verified nor unverified) for email: {}, OTP code: {}", email, otpCode);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP. Please verify OTP first.");
            }
        } else {
            PasswordResetOtp otp = otpOpt.get();
            log.debug("Found verified OTP. OTP expires at: {}, Current time: {}", otp.getExpiresAt(), now);
        }
        
        // Tìm user theo email
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        User user = userOpt.get();

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successful for: {}", email);
    }

    /**
     * Generate 6-digit OTP code
     */
    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // 100000-999999
        return String.valueOf(otp);
    }
}

