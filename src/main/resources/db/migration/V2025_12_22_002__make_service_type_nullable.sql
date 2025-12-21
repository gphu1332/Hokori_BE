-- Migration: Make service_type column nullable in ai_quotas
-- This is required for unified request pool where service_type is deprecated

DO $$
BEGIN
    -- Make service_type nullable
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'ai_quotas' 
               AND column_name = 'service_type' 
               AND is_nullable = 'NO') THEN
        ALTER TABLE ai_quotas ALTER COLUMN service_type DROP NOT NULL;
        RAISE NOTICE 'Made service_type nullable in ai_quotas';
    ELSE
        RAISE NOTICE 'service_type is already nullable or does not exist';
    END IF;
    
    -- Set service_type to NULL for all records (cleanup legacy data)
    UPDATE ai_quotas SET service_type = NULL WHERE service_type IS NOT NULL;
    RAISE NOTICE 'Set service_type to NULL for all existing records';
END $$;

