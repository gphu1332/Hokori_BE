-- ============================================
-- REMOVE FAILED_ATTEMPTS COLUMN FROM PASSWORD_RESET_OTP
-- ============================================
-- Column này không còn cần thiết vì đã dùng bảng password_reset_failed_attempts riêng
-- ============================================

-- Xóa column failed_attempts nếu tồn tại
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'password_reset_otp' 
        AND column_name = 'failed_attempts'
    ) THEN
        ALTER TABLE password_reset_otp DROP COLUMN failed_attempts;
        RAISE NOTICE 'Dropped column failed_attempts from password_reset_otp';
    ELSE
        RAISE NOTICE 'Column failed_attempts does not exist in password_reset_otp';
    END IF;
END $$;

-- Xóa constraint liên quan nếu có
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_otp_failed_attempts' 
        AND conrelid = 'password_reset_otp'::regclass
    ) THEN
        ALTER TABLE password_reset_otp DROP CONSTRAINT ck_otp_failed_attempts;
        RAISE NOTICE 'Dropped constraint ck_otp_failed_attempts';
    END IF;
END $$;

