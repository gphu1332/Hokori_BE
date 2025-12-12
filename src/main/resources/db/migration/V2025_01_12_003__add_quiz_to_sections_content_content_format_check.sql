-- Migration: Add QUIZ to sections_content_content_format_check constraint
-- This migration updates the constraint to allow QUIZ as a valid content_format value

-- Step 1: Check for any invalid data and fix it
-- If there are any rows with invalid content_format values, set them to ASSET (default)
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM sections_content
    WHERE content_format NOT IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ');
    
    IF invalid_count > 0 THEN
        RAISE NOTICE 'Found % rows with invalid content_format, updating to ASSET', invalid_count;
        UPDATE sections_content
        SET content_format = 'ASSET'
        WHERE content_format NOT IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ');
    ELSE
        RAISE NOTICE 'No invalid content_format values found';
    END IF;
END $$;

-- Step 2: Drop existing constraint if it exists
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

-- Step 3: Add new constraint with QUIZ included
-- Only add if no rows violate the constraint
DO $$
DECLARE
    violation_count INTEGER;
BEGIN
    -- Check if any rows would violate the new constraint
    SELECT COUNT(*) INTO violation_count
    FROM sections_content
    WHERE content_format NOT IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ');
    
    IF violation_count = 0 THEN
        ALTER TABLE sections_content 
        ADD CONSTRAINT sections_content_content_format_check 
        CHECK (content_format IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ'));
        RAISE NOTICE 'Successfully added sections_content_content_format_check constraint';
    ELSE
        RAISE EXCEPTION 'Cannot add constraint: % rows still violate the constraint', violation_count;
    END IF;
END $$;

