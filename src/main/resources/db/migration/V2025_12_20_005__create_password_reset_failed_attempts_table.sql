-- ============================================
-- CREATE PASSWORD RESET FAILED ATTEMPTS TABLE
-- ============================================
-- Bảng này dùng để track mỗi lần verify OTP sai
-- Đơn giản: mỗi lần verify sai → insert một record
-- Đếm số record trong 15 phút gần đây, nếu >= 5 → lockout
-- ============================================

CREATE TABLE IF NOT EXISTS password_reset_failed_attempts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    otp_id BIGINT NULL,  -- Reference to password_reset_otp.id (optional, for debugging)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes để tối ưu query performance
CREATE INDEX IF NOT EXISTS idx_failed_attempts_email ON password_reset_failed_attempts(email);
CREATE INDEX IF NOT EXISTS idx_failed_attempts_email_attempted ON password_reset_failed_attempts(email, attempted_at DESC);
CREATE INDEX IF NOT EXISTS idx_failed_attempts_attempted_at ON password_reset_failed_attempts(attempted_at);

COMMENT ON TABLE password_reset_failed_attempts IS 'Bảng track mỗi lần verify OTP sai. Đếm số record trong 15 phút để lockout.';
COMMENT ON COLUMN password_reset_failed_attempts.email IS 'Email của user verify sai OTP';
COMMENT ON COLUMN password_reset_failed_attempts.ip_address IS 'IP address của user (nullable)';
COMMENT ON COLUMN password_reset_failed_attempts.attempted_at IS 'Thời gian verify sai OTP';
COMMENT ON COLUMN password_reset_failed_attempts.otp_id IS 'Reference to password_reset_otp.id (optional, for debugging)';

