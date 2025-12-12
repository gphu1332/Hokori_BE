-- Migration: Add QUIZ to sections_content_content_format_check constraint
-- This migration updates the constraint to allow QUIZ as a valid content_format value

-- Step 1: Drop existing constraint if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'sections_content_content_format_check'
    ) THEN
        ALTER TABLE sections_content DROP CONSTRAINT sections_content_content_format_check;
        RAISE NOTICE 'Dropped existing sections_content_content_format_check constraint';
    ELSE
        RAISE NOTICE 'sections_content_content_format_check constraint does not exist, skipping drop';
    END IF;
END $$;

-- Step 2: Add new constraint with QUIZ included
ALTER TABLE sections_content 
ADD CONSTRAINT sections_content_content_format_check 
CHECK (content_format IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ'));

