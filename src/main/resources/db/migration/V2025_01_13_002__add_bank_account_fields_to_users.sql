-- Migration: Add bank account fields and payout tracking to users table
-- These fields are used for teacher revenue payout management

-- Step 1: Add bank account fields (nullable, teachers can update later)
DO $$ 
BEGIN
    -- bank_account_number
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'bank_account_number'
    ) THEN
        ALTER TABLE users ADD COLUMN bank_account_number VARCHAR(100) NULL;
    END IF;
    
    -- bank_account_name
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'bank_account_name'
    ) THEN
        ALTER TABLE users ADD COLUMN bank_account_name VARCHAR(150) NULL;
    END IF;
    
    -- bank_name
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'bank_name'
    ) THEN
        ALTER TABLE users ADD COLUMN bank_name VARCHAR(150) NULL;
    END IF;
    
    -- bank_branch_name
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'bank_branch_name'
    ) THEN
        ALTER TABLE users ADD COLUMN bank_branch_name VARCHAR(150) NULL;
    END IF;
    
    -- last_payout_date
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'last_payout_date'
    ) THEN
        ALTER TABLE users ADD COLUMN last_payout_date DATE NULL;
    END IF;
    
    -- wallet_balance (for tracking teacher earnings, default 0)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'wallet_balance'
    ) THEN
        ALTER TABLE users ADD COLUMN wallet_balance BIGINT NOT NULL DEFAULT 0;
    ELSE
        -- Update existing NULL values to 0
        UPDATE users SET wallet_balance = 0 WHERE wallet_balance IS NULL;
        -- Make it NOT NULL if it was nullable
        ALTER TABLE users ALTER COLUMN wallet_balance SET NOT NULL;
        ALTER TABLE users ALTER COLUMN wallet_balance SET DEFAULT 0;
    END IF;
END $$;

-- Step 2: Add comments
COMMENT ON COLUMN users.bank_account_number IS 'Số tài khoản ngân hàng của teacher (để nhận thanh toán)';
COMMENT ON COLUMN users.bank_account_name IS 'Tên chủ tài khoản ngân hàng';
COMMENT ON COLUMN users.bank_name IS 'Tên ngân hàng';
COMMENT ON COLUMN users.bank_branch_name IS 'Tên chi nhánh ngân hàng';
COMMENT ON COLUMN users.last_payout_date IS 'Ngày trả tiền gần nhất cho teacher';
COMMENT ON COLUMN users.wallet_balance IS 'Số dư ví của teacher (tính bằng cents, default 0)';

