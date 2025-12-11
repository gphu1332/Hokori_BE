package com.hokori.web.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service để gửi SMS OTP
 * 
 * Có thể tích hợp với:
 * - Twilio (phổ biến nhất)
 * - AWS SNS
 * - Service khác
 * 
 * Hiện tại: Mock implementation (log OTP để test)
 * Để production: Cần tích hợp với service thật
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:mock}")
    private String smsProvider; // mock, twilio, aws-sns

    /**
     * Gửi OTP qua SMS
     */
    public void sendOtpSms(String phoneNumber, String otpCode) {
        if (!smsEnabled) {
            log.warn("SMS service is disabled. OTP for {}: {}", phoneNumber, otpCode);
            return;
        }

        try {
            switch (smsProvider.toLowerCase()) {
                case "twilio" -> sendViaTwilio(phoneNumber, otpCode);
                case "aws-sns" -> sendViaAwsSns(phoneNumber, otpCode);
                default -> {
                    // Mock: chỉ log OTP (cho development/testing)
                    log.info("SMS OTP (MOCK) - Phone: {}, OTP: {}", phoneNumber, otpCode);
                }
            }
            log.info("OTP SMS sent successfully to: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send OTP SMS to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Gửi SMS qua Twilio (cần tích hợp)
     */
    private void sendViaTwilio(String phoneNumber, String otpCode) {
        // TODO: Implement Twilio integration
        // TwilioClient twilio = Twilio.init(accountSid, authToken);
        // Message message = Message.creator(
        //     new PhoneNumber(phoneNumber),
        //     new PhoneNumber(twilioPhoneNumber),
        //     "Your Hokori OTP code is: " + otpCode
        // ).create();
        log.warn("Twilio integration not implemented yet. OTP for {}: {}", phoneNumber, otpCode);
    }

    /**
     * Gửi SMS qua AWS SNS (cần tích hợp)
     */
    private void sendViaAwsSns(String phoneNumber, String otpCode) {
        // TODO: Implement AWS SNS integration
        log.warn("AWS SNS integration not implemented yet. OTP for {}: {}", phoneNumber, otpCode);
    }
}

