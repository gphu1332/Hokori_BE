-- Migration: Add CONVERSATION to ai_quotas_service_type_check constraint
-- This migration updates the constraint to allow CONVERSATION as a valid service_type value

-- Step 1: Drop existing constraint if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ai_quotas_service_type_check'
        AND conrelid = 'ai_quotas'::regclass
    ) THEN
        ALTER TABLE ai_quotas DROP CONSTRAINT ai_quotas_service_type_check;
        RAISE NOTICE 'Dropped existing ai_quotas_service_type_check constraint';
    ELSE
        RAISE NOTICE 'ai_quotas_service_type_check constraint does not exist, skipping drop';
    END IF;
END $$;

-- Step 2: Add new constraint with CONVERSATION included (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ai_quotas_service_type_check'
        AND conrelid = 'ai_quotas'::regclass
    ) THEN
        ALTER TABLE ai_quotas 
        ADD CONSTRAINT ai_quotas_service_type_check 
        CHECK (service_type IN ('GRAMMAR', 'KAIWA', 'PRONUN', 'CONVERSATION'));
        RAISE NOTICE 'Added ai_quotas_service_type_check constraint with CONVERSATION';
    ELSE
        RAISE NOTICE 'ai_quotas_service_type_check constraint already exists, skipping add';
    END IF;
END $$;

