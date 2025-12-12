-- Migration: Add QUIZ to sections_study_type_check constraint
-- This migration updates the constraint to allow QUIZ as a valid study_type value

-- Step 1: Drop existing constraint if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'sections_study_type_check'
    ) THEN
        ALTER TABLE sections DROP CONSTRAINT sections_study_type_check;
        RAISE NOTICE 'Dropped existing sections_study_type_check constraint';
    ELSE
        RAISE NOTICE 'sections_study_type_check constraint does not exist, skipping drop';
    END IF;
END $$;

-- Step 2: Add new constraint with QUIZ included
ALTER TABLE sections 
ADD CONSTRAINT sections_study_type_check 
CHECK (study_type IN ('GRAMMAR', 'VOCABULARY', 'KANJI', 'QUIZ'));

