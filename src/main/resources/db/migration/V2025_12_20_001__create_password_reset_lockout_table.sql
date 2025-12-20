-- ============================================
-- CREATE PASSWORD RESET LOCKOUT TABLE
-- ============================================
-- Bảng này dùng để track lockout cho password reset khi brute-force attack
-- Khi user nhập sai OTP quá 5 lần, hệ thống sẽ khóa chức năng forgot password
-- cho email/IP đó trong 30 phút
-- ============================================

CREATE TABLE IF NOT EXISTS password_reset_lockout (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NULL,                    -- Email bị khóa (nullable - có thể khóa theo IP mà không cần email)
    ip_address VARCHAR(45) NULL,                 -- IP address bị khóa (nullable - có thể khóa theo email mà không cần IP)
    locked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- Thời gian bắt đầu khóa
    unlock_at TIMESTAMP NOT NULL,                -- Thời gian mở khóa (thường là 30 phút sau khi locked_at)
    reason VARCHAR(500) NULL,                     -- Lý do khóa (ví dụ: "Too many failed OTP attempts")
    is_unlocked BOOLEAN NOT NULL DEFAULT FALSE,  -- Đã mở khóa chưa (có thể mở khóa thủ công trước khi hết hạn)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- Thời gian tạo record
);

-- Indexes để tối ưu query performance
CREATE INDEX IF NOT EXISTS idx_lockout_email ON password_reset_lockout(email);
CREATE INDEX IF NOT EXISTS idx_lockout_ip ON password_reset_lockout(ip_address);
CREATE INDEX IF NOT EXISTS idx_lockout_unlock_at ON password_reset_lockout(unlock_at);

-- Index composite để tối ưu query tìm lockout active
CREATE INDEX IF NOT EXISTS idx_lockout_active ON password_reset_lockout(is_unlocked, unlock_at) 
    WHERE is_unlocked = FALSE;

COMMENT ON TABLE password_reset_lockout IS 'Bảng track lockout cho password reset khi brute-force attack';
COMMENT ON COLUMN password_reset_lockout.email IS 'Email bị khóa (nullable - có thể khóa theo IP mà không cần email)';
COMMENT ON COLUMN password_reset_lockout.ip_address IS 'IP address bị khóa (nullable - có thể khóa theo email mà không cần IP)';
COMMENT ON COLUMN password_reset_lockout.locked_at IS 'Thời gian bắt đầu khóa';
COMMENT ON COLUMN password_reset_lockout.unlock_at IS 'Thời gian mở khóa (thường là 30 phút sau khi locked_at)';
COMMENT ON COLUMN password_reset_lockout.reason IS 'Lý do khóa (ví dụ: "Too many failed OTP attempts")';
COMMENT ON COLUMN password_reset_lockout.is_unlocked IS 'Đã mở khóa chưa (có thể mở khóa thủ công trước khi hết hạn)';

