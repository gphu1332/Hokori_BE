-- Migration: Add pending update support for published courses
-- Allows teachers to submit updates to published courses without hiding them
-- Course remains PUBLISHED with old content while update is pending approval

-- Step 1: Add pending_update_at column to course table
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'course' AND column_name = 'pending_update_at'
    ) THEN
        ALTER TABLE course ADD COLUMN pending_update_at TIMESTAMP NULL;
    END IF;
END $$;

-- Step 2: Update course_status_check constraint to include PENDING_UPDATE
DO $$
BEGIN
    -- Drop existing constraint if exists
    ALTER TABLE course DROP CONSTRAINT IF EXISTS course_status_check;
    
    -- Add new constraint with PENDING_UPDATE
    ALTER TABLE course 
    ADD CONSTRAINT course_status_check 
    CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'PUBLISHED', 'PENDING_UPDATE', 'FLAGGED', 'ARCHIVED'));
END $$;

-- Step 3: Add comment
COMMENT ON COLUMN course.pending_update_at IS 'Timestamp when teacher submitted update for published course. Course remains PUBLISHED with old content until moderator approves update.';

