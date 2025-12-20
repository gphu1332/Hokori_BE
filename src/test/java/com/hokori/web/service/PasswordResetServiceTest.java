package com.hokori.web.service;

import com.hokori.web.entity.*;
import com.hokori.web.exception.InvalidOtpException;
import com.hokori.web.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetOtpRepository otpRepository;
    @Mock private PasswordResetLockoutRepository lockoutRepository;
    @Mock private PasswordResetFailedAttemptRepository failedAttemptRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EntityManager entityManager;

    // ⚠️ self-injection mock
    @Mock private PasswordResetService self;

    @InjectMocks
    private PasswordResetService service;

    private final String EMAIL = "test@hokori.com";

    @BeforeEach
    void setup() {
        // Quan trọng: self phải trỏ về chính service
        Mockito.lenient()
                .doCallRealMethod()
                .when(self)
                .recordFailedAttempt(
                        anyString(),
                        any(),
                        any(),
                        anyLong()
                );
    }

    /* =========================================================
       REQUEST OTP
       ========================================================= */

    @Test
    void requestOtp_success() {
        User user = mockUser();

        when(lockoutRepository.findActiveLockoutByEmailOrIp(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        service.requestOtpByEmail(EMAIL);

        verify(otpRepository).invalidateOldOtpsForEmail(eq(EMAIL), any());
        verify(otpRepository).save(any(PasswordResetOtp.class));
        verify(emailService).sendOtpEmail(eq(EMAIL), anyString());
    }

    @Test
    void requestOtp_userNotFound_shouldSilentReturn() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.requestOtpByEmail(EMAIL));
    }

    /* =========================================================
       VERIFY OTP
       ========================================================= */

    @Test
    void verifyOtp_success() {
        PasswordResetOtp otp = mockOtp("123456");

        when(lockoutRepository.findActiveLockoutByEmailOrIp(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(otpRepository.findLatestValidByEmail(eq(EMAIL), any()))
                .thenReturn(Optional.of(otp));

        String result = service.verifyOtp(EMAIL, "123456");

        assertEquals(EMAIL, result);
        verify(otpRepository).markAsUsed(otp.getId());
    }

    @Test
    void verifyOtp_wrongOtp_increaseFailedAttempts() {
        PasswordResetOtp otp = mockOtp("111111");

        when(lockoutRepository.findActiveLockoutByEmailOrIp(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(otpRepository.findLatestValidByEmail(eq(EMAIL), any()))
                .thenReturn(Optional.of(otp));
        when(failedAttemptRepository.countFailedAttemptsByEmailSince(any(), any()))
                .thenReturn(2L);

        InvalidOtpException ex = assertThrows(
                InvalidOtpException.class,
                () -> service.verifyOtp(EMAIL, "999999")
        );

        assertEquals(3, ex.getFailedAttempts());
        verify(self).recordFailedAttempt(eq(EMAIL), any(), any(), eq(otp.getId()));
    }

    @Test
    void verifyOtp_reachMaxAttempts_shouldLockout() {
        PasswordResetOtp otp = mockOtp("111111");

        when(lockoutRepository.findActiveLockoutByEmailOrIp(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(otpRepository.findLatestValidByEmail(eq(EMAIL), any()))
                .thenReturn(Optional.of(otp));
        when(failedAttemptRepository.countFailedAttemptsByEmailSince(any(), any()))
                .thenReturn(4L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.verifyOtp(EMAIL, "000000")
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(lockoutRepository).save(any(PasswordResetLockout.class));
    }

    /* =========================================================
       RESET PASSWORD
       ========================================================= */

    @Test
    void resetPassword_success_withVerifiedOtp() {
        PasswordResetOtp otp = mockOtp("123456");
        otp.setIsUsed(true);

        User user = mockUser();

        when(otpRepository.findVerifiedOtpByEmailAndCode(EMAIL, "123456"))
                .thenReturn(Optional.of(otp));
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass"))
                .thenReturn("hashed");

        service.resetPassword(EMAIL, "123456", "newPass");

        assertEquals("hashed", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    /* =========================================================
       HELPERS
       ========================================================= */

    private User mockUser() {
        User u = new User();
        u.setId(1L);
        u.setEmail(EMAIL);
        u.setIsActive(true);
        return u;
    }

    private PasswordResetOtp mockOtp(String code) {
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setId(10L);
        otp.setEmail(EMAIL);
        otp.setOtpCode(code);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otp.setIsUsed(false);
        return otp;
    }
}
