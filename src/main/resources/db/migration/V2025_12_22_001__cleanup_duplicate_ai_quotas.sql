-- Migration: Cleanup duplicate ai_quotas records
-- This migration removes duplicate quota records, keeping only one per user
-- Safe to run multiple times (idempotent)

-- Step 1: For each user with multiple quotas, keep only the first one
DO $$
DECLARE
    user_record RECORD;
    first_id BIGINT;
    deleted_count INTEGER := 0;
BEGIN
    -- Loop through users who have multiple quota records
    FOR user_record IN 
        SELECT user_id, COUNT(*) as quota_count
        FROM ai_quotas
        GROUP BY user_id
        HAVING COUNT(*) > 1
    LOOP
        -- Get the ID of the first quota for this user
        SELECT id INTO first_id
        FROM ai_quotas
        WHERE user_id = user_record.user_id
        ORDER BY id ASC
        LIMIT 1;
        
        -- Delete all other quota records for this user
        DELETE FROM ai_quotas
        WHERE user_id = user_record.user_id
          AND id != first_id;
        
        GET DIAGNOSTICS deleted_count = ROW_COUNT;
        
        RAISE NOTICE 'Cleaned up % duplicate quotas for user_id=%', deleted_count, user_record.user_id;
    END LOOP;
    
    RAISE NOTICE 'Cleanup completed: Removed duplicate ai_quotas records';
END $$;

-- Step 2: Ensure unique constraint exists
DO $$
BEGIN
    -- Drop old uk_user_service constraint if exists
    IF EXISTS (SELECT 1 FROM pg_constraint 
               WHERE conname = 'uk_user_service' 
               AND conrelid = 'ai_quotas'::regclass) THEN
        ALTER TABLE ai_quotas DROP CONSTRAINT uk_user_service;
        RAISE NOTICE 'Dropped uk_user_service constraint';
    END IF;
    
    -- Add uk_ai_quota_user constraint if not exists
    IF NOT EXISTS (SELECT 1 FROM pg_constraint 
                   WHERE conname = 'uk_ai_quota_user' 
                   AND conrelid = 'ai_quotas'::regclass) THEN
        ALTER TABLE ai_quotas ADD CONSTRAINT uk_ai_quota_user UNIQUE (user_id);
        RAISE NOTICE 'Added uk_ai_quota_user UNIQUE constraint';
    ELSE
        RAISE NOTICE 'uk_ai_quota_user constraint already exists';
    END IF;
END $$;

