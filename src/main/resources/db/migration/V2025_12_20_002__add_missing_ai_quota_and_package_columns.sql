-- ============================================
-- ADD MISSING COLUMNS TO AI_QUOTAS AND AI_PACKAGES
-- ============================================
-- Migration này đảm bảo các column total_requests và remaining_requests
-- được thêm vào database nếu migration trước đó chưa chạy hoặc bị lỗi
-- ============================================

-- Step 1: Add missing columns to ai_quotas table
DO $$
BEGIN
    -- Add total_requests column if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_quotas' AND column_name = 'total_requests') THEN
        ALTER TABLE ai_quotas ADD COLUMN total_requests INTEGER NULL;
        RAISE NOTICE 'Added total_requests column to ai_quotas';
    ELSE
        RAISE NOTICE 'Column total_requests already exists in ai_quotas';
    END IF;
    
    -- Add used_requests column if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_quotas' AND column_name = 'used_requests') THEN
        ALTER TABLE ai_quotas ADD COLUMN used_requests INTEGER DEFAULT 0 NOT NULL;
        RAISE NOTICE 'Added used_requests column to ai_quotas';
    ELSE
        RAISE NOTICE 'Column used_requests already exists in ai_quotas';
    END IF;
    
    -- Add remaining_requests column if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_quotas' AND column_name = 'remaining_requests') THEN
        ALTER TABLE ai_quotas ADD COLUMN remaining_requests INTEGER NULL;
        RAISE NOTICE 'Added remaining_requests column to ai_quotas';
    ELSE
        RAISE NOTICE 'Column remaining_requests already exists in ai_quotas';
    END IF;
    
    -- Add last_reset_at column if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_quotas' AND column_name = 'last_reset_at') THEN
        ALTER TABLE ai_quotas ADD COLUMN last_reset_at TIMESTAMP NULL;
        RAISE NOTICE 'Added last_reset_at column to ai_quotas';
    ELSE
        RAISE NOTICE 'Column last_reset_at already exists in ai_quotas';
    END IF;
END $$;

-- Step 2: Add missing columns to ai_packages table
DO $$
BEGIN
    -- Add total_requests column if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_packages' AND column_name = 'total_requests') THEN
        ALTER TABLE ai_packages ADD COLUMN total_requests INTEGER NULL;
        RAISE NOTICE 'Added total_requests column to ai_packages';
    ELSE
        RAISE NOTICE 'Column total_requests already exists in ai_packages';
    END IF;
END $$;

-- Step 3: Initialize values for existing records if needed
-- Set default values for ai_quotas if columns are null
DO $$
BEGIN
    -- Initialize used_requests to 0 if null
    UPDATE ai_quotas 
    SET used_requests = 0 
    WHERE used_requests IS NULL;
    
    -- Calculate remaining_requests from total_requests and used_requests if null
    UPDATE ai_quotas 
    SET remaining_requests = CASE 
        WHEN total_requests IS NULL THEN NULL
        ELSE GREATEST(0, total_requests - COALESCE(used_requests, 0))
    END
    WHERE remaining_requests IS NULL;
    
    RAISE NOTICE 'Initialized values for existing ai_quotas records';
END $$;

-- Step 4: Add comments for documentation
DO $$
BEGIN
    COMMENT ON COLUMN ai_quotas.total_requests IS 'Total unified requests allocated to user (null = unlimited)';
    COMMENT ON COLUMN ai_quotas.used_requests IS 'Number of requests used by user';
    COMMENT ON COLUMN ai_quotas.remaining_requests IS 'Remaining requests available (calculated: total_requests - used_requests)';
    COMMENT ON COLUMN ai_quotas.last_reset_at IS 'Last reset date (for monthly quotas)';
    COMMENT ON COLUMN ai_packages.total_requests IS 'Total unified requests in package (null = unlimited)';
    
    RAISE NOTICE 'Added column comments';
END $$;

