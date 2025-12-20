package com.hokori.web.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Custom exception cho invalid OTP với thông tin về failed attempts
 */
@Getter
public class InvalidOtpException extends ResponseStatusException {
    private final Integer failedAttempts;
    private final Integer remainingAttempts;
    private final Integer maxAttempts;

    public InvalidOtpException(Integer failedAttempts, Integer maxAttempts) {
        super(HttpStatus.BAD_REQUEST, "Invalid OTP code");
        this.failedAttempts = failedAttempts;
        this.maxAttempts = maxAttempts;
        this.remainingAttempts = maxAttempts - failedAttempts;
    }
}

