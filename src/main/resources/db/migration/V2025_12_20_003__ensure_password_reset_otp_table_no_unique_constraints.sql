-- ============================================
-- ENSURE PASSWORD RESET OTP TABLE HAS NO UNIQUE CONSTRAINTS
-- ============================================
-- Migration này đảm bảo table password_reset_otp không có unique constraint
-- để cho phép nhiều OTP cho cùng một email (cần thiết cho business logic)
-- ============================================

-- Tạo table nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS password_reset_otp (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Drop unique constraint trên email nếu có (cho phép nhiều OTP cho cùng email)
DO $$
BEGIN
    -- Drop unique constraint trên email nếu tồn tại
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname LIKE '%email%' 
        AND conrelid = 'password_reset_otp'::regclass
        AND contype = 'u'  -- u = unique constraint
    ) THEN
        -- Tìm và drop tất cả unique constraints liên quan đến email
        DECLARE
            constraint_name TEXT;
        BEGIN
            FOR constraint_name IN
                SELECT conname FROM pg_constraint
                WHERE conrelid = 'password_reset_otp'::regclass
                AND contype = 'u'
                AND (
                    conname LIKE '%email%' 
                    OR EXISTS (
                        SELECT 1 FROM pg_attribute 
                        WHERE attrelid = conrelid 
                        AND attname = 'email' 
                        AND attnum = ANY(conkey)
                    )
                )
            LOOP
                EXECUTE 'ALTER TABLE password_reset_otp DROP CONSTRAINT IF EXISTS ' || quote_ident(constraint_name);
                RAISE NOTICE 'Dropped unique constraint: %', constraint_name;
            END LOOP;
        END;
    END IF;
END $$;

-- Drop unique constraint trên otp_code nếu có (cho phép nhiều OTP cùng code cho các email khác nhau)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname LIKE '%otp_code%' 
        AND conrelid = 'password_reset_otp'::regclass
        AND contype = 'u'
    ) THEN
        DECLARE
            constraint_name TEXT;
        BEGIN
            FOR constraint_name IN
                SELECT conname FROM pg_constraint
                WHERE conrelid = 'password_reset_otp'::regclass
                AND contype = 'u'
                AND (
                    conname LIKE '%otp_code%'
                    OR EXISTS (
                        SELECT 1 FROM pg_attribute 
                        WHERE attrelid = conrelid 
                        AND attname = 'otp_code' 
                        AND attnum = ANY(conkey)
                    )
                )
            LOOP
                EXECUTE 'ALTER TABLE password_reset_otp DROP CONSTRAINT IF EXISTS ' || quote_ident(constraint_name);
                RAISE NOTICE 'Dropped unique constraint: %', constraint_name;
            END LOOP;
        END;
    END IF;
END $$;

-- Drop unique constraint composite (email, otp_code) nếu có
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conrelid = 'password_reset_otp'::regclass
        AND contype = 'u'
        AND array_length(conkey, 1) > 1  -- Composite constraint
    ) THEN
        DECLARE
            constraint_name TEXT;
        BEGIN
            FOR constraint_name IN
                SELECT conname FROM pg_constraint
                WHERE conrelid = 'password_reset_otp'::regclass
                AND contype = 'u'
                AND array_length(conkey, 1) > 1
            LOOP
                EXECUTE 'ALTER TABLE password_reset_otp DROP CONSTRAINT IF EXISTS ' || quote_ident(constraint_name);
                RAISE NOTICE 'Dropped composite unique constraint: %', constraint_name;
            END LOOP;
        END;
    END IF;
END $$;

-- Đảm bảo các indexes cần thiết tồn tại (không phải unique)
CREATE INDEX IF NOT EXISTS idx_otp_email ON password_reset_otp(email);
CREATE INDEX IF NOT EXISTS idx_otp_code ON password_reset_otp(otp_code);
CREATE INDEX IF NOT EXISTS idx_otp_email_created ON password_reset_otp(email, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_otp_expires ON password_reset_otp(expires_at);

-- Thêm check constraints để đảm bảo data integrity
-- Check: otp_code phải là đúng 6 chữ số (0-9)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_otp_code_format' 
        AND conrelid = 'password_reset_otp'::regclass
    ) THEN
        ALTER TABLE password_reset_otp 
        ADD CONSTRAINT ck_otp_code_format 
        CHECK (otp_code ~ '^[0-9]{6}$');
        RAISE NOTICE 'Added check constraint: ck_otp_code_format';
    END IF;
END $$;

-- Check: failed_attempts phải >= 0
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_otp_failed_attempts' 
        AND conrelid = 'password_reset_otp'::regclass
    ) THEN
        ALTER TABLE password_reset_otp 
        ADD CONSTRAINT ck_otp_failed_attempts 
        CHECK (failed_attempts >= 0);
        RAISE NOTICE 'Added check constraint: ck_otp_failed_attempts';
    END IF;
END $$;

-- Check: expires_at phải > created_at (OTP phải có thời gian hết hạn hợp lý)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_otp_expires_after_created' 
        AND conrelid = 'password_reset_otp'::regclass
    ) THEN
        ALTER TABLE password_reset_otp 
        ADD CONSTRAINT ck_otp_expires_after_created 
        CHECK (expires_at > created_at);
        RAISE NOTICE 'Added check constraint: ck_otp_expires_after_created';
    END IF;
END $$;

-- Check: email không được rỗng (đã có NOT NULL nhưng thêm check để đảm bảo)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_otp_email_not_empty' 
        AND conrelid = 'password_reset_otp'::regclass
    ) THEN
        ALTER TABLE password_reset_otp 
        ADD CONSTRAINT ck_otp_email_not_empty 
        CHECK (LENGTH(TRIM(email)) > 0);
        RAISE NOTICE 'Added check constraint: ck_otp_email_not_empty';
    END IF;
END $$;

COMMENT ON TABLE password_reset_otp IS 'Bảng lưu mã OTP cho password reset. Cho phép nhiều OTP cho cùng email (không có unique constraint).';
COMMENT ON COLUMN password_reset_otp.email IS 'Email của user (không unique - cho phép nhiều OTP, NOT NULL)';
COMMENT ON COLUMN password_reset_otp.otp_code IS 'Mã OTP 6 chữ số (không unique - cho phép trùng code cho các email khác nhau, format: ^[0-9]{6}$)';
COMMENT ON COLUMN password_reset_otp.failed_attempts IS 'Số lần verify sai (>= 0, default: 0)';
COMMENT ON COLUMN password_reset_otp.expires_at IS 'Thời gian hết hạn (phải > created_at)';

