-- Migration: Migrate from separate service quotas to unified request pool
-- This migration consolidates all service-specific quotas into a single unified request pool per user

-- Step 1: Create new unified quota structure
-- First, add new columns to ai_quotas table (keeping old columns for migration)
DO $$
BEGIN
    -- Add unified request pool columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_quotas' AND column_name = 'total_requests') THEN
        ALTER TABLE ai_quotas ADD COLUMN total_requests INTEGER NULL;
        RAISE NOTICE 'Added total_requests column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_quotas' AND column_name = 'used_requests') THEN
        ALTER TABLE ai_quotas ADD COLUMN used_requests INTEGER DEFAULT 0;
        RAISE NOTICE 'Added used_requests column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_quotas' AND column_name = 'remaining_requests') THEN
        ALTER TABLE ai_quotas ADD COLUMN remaining_requests INTEGER NULL;
        RAISE NOTICE 'Added remaining_requests column';
    END IF;
END $$;

-- Step 2: Migrate existing data to unified pool
-- For each user, sum up all remaining quotas from all service types
DO $$
DECLARE
    user_record RECORD;
    total_remaining INTEGER;
    total_allocated INTEGER;
BEGIN
    -- Loop through all users
    FOR user_record IN SELECT DISTINCT user_id FROM ai_quotas LOOP
        -- Calculate total remaining quota across all services
        SELECT COALESCE(SUM(remaining_quota), 0) INTO total_remaining
        FROM ai_quotas
        WHERE user_id = user_record.user_id
          AND remaining_quota IS NOT NULL;
        
        -- Calculate total allocated quota across all services
        SELECT COALESCE(SUM(total_quota), 0) INTO total_allocated
        FROM ai_quotas
        WHERE user_id = user_record.user_id
          AND total_quota IS NOT NULL;
        
        -- Update the first quota record with unified values
        -- Keep only one record per user (delete others later)
        UPDATE ai_quotas
        SET total_requests = total_allocated,
            used_requests = total_allocated - total_remaining,
            remaining_requests = total_remaining
        WHERE id = (
            SELECT id FROM ai_quotas
            WHERE user_id = user_record.user_id
            ORDER BY id ASC
            LIMIT 1
        );
        
        -- Log migration progress
        RAISE NOTICE 'Migrated user_id: %, total_requests: %, used_requests: %, remaining_requests: %', 
            user_record.user_id, total_allocated, (total_allocated - total_remaining), total_remaining;
    END LOOP;
END $$;

-- Step 3: Delete duplicate quota records (keep only one per user)
-- Delete all records except the first one for each user
DO $$
DECLARE
    user_record RECORD;
    first_id BIGINT;
BEGIN
    FOR user_record IN SELECT DISTINCT user_id FROM ai_quotas LOOP
        -- Get the first quota ID for this user
        SELECT id INTO first_id
        FROM ai_quotas
        WHERE user_id = user_record.user_id
        ORDER BY id ASC
        LIMIT 1;
        
        -- Delete all other quota records for this user
        DELETE FROM ai_quotas
        WHERE user_id = user_record.user_id
          AND id != first_id;
        
        RAISE NOTICE 'Cleaned up duplicate quotas for user_id=%', user_record.user_id;
    END LOOP;
END $$;

-- Step 4: Drop old constraints and indexes
DO $$
BEGIN
    -- Drop unique constraint on (user_id, service_type)
    IF EXISTS (SELECT 1 FROM pg_constraint 
               WHERE conname = 'uk_user_service' 
               AND conrelid = 'ai_quotas'::regclass) THEN
        ALTER TABLE ai_quotas DROP CONSTRAINT uk_user_service;
        RAISE NOTICE 'Dropped uk_user_service constraint';
    END IF;
    
    -- Drop service_type check constraint
    IF EXISTS (SELECT 1 FROM pg_constraint 
               WHERE conname = 'ai_quotas_service_type_check' 
               AND conrelid = 'ai_quotas'::regclass) THEN
        ALTER TABLE ai_quotas DROP CONSTRAINT ai_quotas_service_type_check;
        RAISE NOTICE 'Dropped ai_quotas_service_type_check constraint';
    END IF;
    
    -- Drop service_type index
    IF EXISTS (SELECT 1 FROM pg_indexes 
               WHERE tablename = 'ai_quotas' 
               AND indexname = 'idx_ai_quota_service') THEN
        DROP INDEX idx_ai_quota_service;
        RAISE NOTICE 'Dropped idx_ai_quota_service index';
    END IF;
END $$;

-- Step 5: Add new unique constraint on user_id only (one quota per user)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint 
                   WHERE conname = 'uk_ai_quota_user' 
                   AND conrelid = 'ai_quotas'::regclass) THEN
        ALTER TABLE ai_quotas 
        ADD CONSTRAINT uk_ai_quota_user UNIQUE (user_id);
        RAISE NOTICE 'Added uk_ai_quota_user constraint';
    END IF;
END $$;

-- Step 6: Make service_type nullable (will be removed later, but keep for now for safety)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'ai_quotas' 
               AND column_name = 'service_type' 
               AND is_nullable = 'NO') THEN
        ALTER TABLE ai_quotas ALTER COLUMN service_type DROP NOT NULL;
        RAISE NOTICE 'Made service_type nullable';
    END IF;
END $$;

-- Step 7: Update ai_packages table - add total_requests column
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ai_packages' AND column_name = 'total_requests') THEN
        ALTER TABLE ai_packages ADD COLUMN total_requests INTEGER NULL;
        RAISE NOTICE 'Added total_requests column to ai_packages';
    END IF;
END $$;

-- Step 8: Migrate ai_packages data
-- For packages with separate quotas, calculate total_requests
-- Strategy: Use the maximum quota value as total_requests (or sum if preferred)
DO $$
DECLARE
    package_record RECORD;
    calculated_total INTEGER;
BEGIN
    FOR package_record IN SELECT id, grammar_quota, kaiwa_quota, pronun_quota, conversation_quota 
                          FROM ai_packages 
                          WHERE total_requests IS NULL LOOP
        -- Calculate total: sum of all quotas (or max if all are same)
        -- If any quota is null (unlimited), set total_requests to null
        IF package_record.grammar_quota IS NULL OR 
           package_record.kaiwa_quota IS NULL OR 
           package_record.pronun_quota IS NULL OR 
           package_record.conversation_quota IS NULL THEN
            -- If any is unlimited, set total_requests to null (unlimited)
            calculated_total := NULL;
        ELSE
            -- Sum all quotas
            calculated_total := COALESCE(package_record.grammar_quota, 0) +
                               COALESCE(package_record.kaiwa_quota, 0) +
                               COALESCE(package_record.pronun_quota, 0) +
                               COALESCE(package_record.conversation_quota, 0);
        END IF;
        
        UPDATE ai_packages
        SET total_requests = calculated_total
        WHERE id = package_record.id;
        
        RAISE NOTICE 'Migrated package_id=%, total_requests=%', package_record.id, calculated_total;
    END LOOP;
END $$;

-- Step 9: Add comments for documentation
COMMENT ON COLUMN ai_quotas.total_requests IS 'Total unified requests allocated to user (null = unlimited)';
COMMENT ON COLUMN ai_quotas.used_requests IS 'Number of requests used by user';
COMMENT ON COLUMN ai_quotas.remaining_requests IS 'Remaining requests available (calculated: total_requests - used_requests)';
COMMENT ON COLUMN ai_packages.total_requests IS 'Total unified requests in package (null = unlimited)';

-- Migration completed successfully
DO $$
BEGIN
    RAISE NOTICE 'Migration completed: Unified request pool structure created';
END $$;

