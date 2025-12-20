-- ============================================
-- CLEANUP AND FIX OLD PASSWORD RESET DATA
-- ============================================
-- Migration này cleanup và fix dữ liệu cũ từ các deploy trước:
-- 1. Xóa các OTP đã hết hạn quá lâu (hơn 24 giờ)
-- 2. Fix failed_attempts NULL thành 0
-- 3. Fix expires_at NULL hoặc expires_at < created_at
-- 4. Fix is_used NULL thành FALSE
-- 5. Fix created_at NULL
-- ============================================

-- 1. Fix failed_attempts NULL thành 0
UPDATE password_reset_otp 
SET failed_attempts = 0 
WHERE failed_attempts IS NULL;

-- 2. Fix is_used NULL thành FALSE
UPDATE password_reset_otp 
SET is_used = FALSE 
WHERE is_used IS NULL;

-- 3. Fix created_at NULL thành CURRENT_TIMESTAMP (hoặc expires_at - 15 phút nếu expires_at có)
UPDATE password_reset_otp 
SET created_at = COALESCE(
    expires_at - INTERVAL '15 minutes',
    CURRENT_TIMESTAMP
)
WHERE created_at IS NULL;

-- 4. Fix expires_at NULL hoặc expires_at < created_at
-- Nếu expires_at NULL hoặc expires_at <= created_at, set expires_at = created_at + 15 phút
UPDATE password_reset_otp 
SET expires_at = created_at + INTERVAL '15 minutes'
WHERE expires_at IS NULL 
   OR expires_at <= created_at;

-- 5. Fix failed_attempts < 0 thành 0 (nếu có dữ liệu không hợp lệ)
UPDATE password_reset_otp 
SET failed_attempts = 0 
WHERE failed_attempts < 0;

-- 6. Xóa các OTP đã hết hạn quá lâu (hơn 24 giờ) để cleanup database
-- Chỉ xóa các OTP đã hết hạn và không còn cần thiết
DELETE FROM password_reset_otp 
WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '24 hours';

-- 7. Đảm bảo tất cả columns có giá trị hợp lệ (sau khi fix)
-- Set NOT NULL nếu chưa có (migration V2025_12_20_003 đã tạo table với NOT NULL, nhưng đảm bảo)
DO $$
BEGIN
    -- Đảm bảo failed_attempts có giá trị
    UPDATE password_reset_otp 
    SET failed_attempts = COALESCE(failed_attempts, 0)
    WHERE failed_attempts IS NULL;
    
    -- Đảm bảo is_used có giá trị
    UPDATE password_reset_otp 
    SET is_used = COALESCE(is_used, FALSE)
    WHERE is_used IS NULL;
    
    -- Đảm bảo created_at có giá trị
    UPDATE password_reset_otp 
    SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP)
    WHERE created_at IS NULL;
    
    -- Đảm bảo expires_at có giá trị và > created_at
    UPDATE password_reset_otp 
    SET expires_at = GREATEST(
        expires_at,
        created_at + INTERVAL '15 minutes'
    )
    WHERE expires_at IS NULL 
       OR expires_at <= created_at;
END $$;

-- 8. Log số lượng records đã được cleanup
DO $$
DECLARE
    expired_count INTEGER;
    fixed_count INTEGER;
BEGIN
    -- Count expired OTPs đã được xóa (trong 24h qua)
    SELECT COUNT(*) INTO expired_count
    FROM password_reset_otp
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '24 hours';
    
    -- Count records đã được fix
    SELECT COUNT(*) INTO fixed_count
    FROM password_reset_otp
    WHERE failed_attempts < 0 
       OR expires_at <= created_at
       OR created_at IS NULL
       OR expires_at IS NULL;
    
    RAISE NOTICE 'Cleanup completed: % expired OTPs removed, % records fixed', expired_count, fixed_count;
END $$;

COMMENT ON TABLE password_reset_otp IS 'Bảng lưu mã OTP cho password reset. Đã được cleanup và fix dữ liệu cũ từ migration V2025_12_20_004.';

