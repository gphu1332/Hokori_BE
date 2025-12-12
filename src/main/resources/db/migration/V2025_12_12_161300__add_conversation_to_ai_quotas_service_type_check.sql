-- Migration: Add CONVERSATION to ai_quotas_service_type_check constraint
-- This migration updates the constraint to allow CONVERSATION as a valid service_type value

-- Step 1: Drop ALL existing check constraints on service_type column if they exist
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    -- Find and drop all check constraints on service_type column
    FOR constraint_name IN 
        SELECT conname 
        FROM pg_constraint 
        WHERE conrelid = 'ai_quotas'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%service_type%'
    LOOP
        EXECUTE format('ALTER TABLE ai_quotas DROP CONSTRAINT IF EXISTS %I', constraint_name);
        RAISE NOTICE 'Dropped constraint: %', constraint_name;
    END LOOP;
END $$;

-- Step 2: Add new constraint with CONVERSATION included
ALTER TABLE ai_quotas 
ADD CONSTRAINT ai_quotas_service_type_check 
CHECK (service_type IN ('GRAMMAR', 'KAIWA', 'PRONUN', 'CONVERSATION'));

