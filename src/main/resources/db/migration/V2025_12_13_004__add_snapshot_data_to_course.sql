-- Migration: Add snapshot_data column to course table for storing course tree snapshot
-- Used to preserve old content when course is in PENDING_UPDATE status
-- Learners will see old content from snapshot until moderator approves update

-- Step 1: Add snapshot_data column (JSONB for efficient storage and querying)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'course' AND column_name = 'snapshot_data'
    ) THEN
        ALTER TABLE course ADD COLUMN snapshot_data JSONB NULL;
    END IF;
END $$;

-- Step 2: Add index for snapshot_data (optional, but useful if we need to query by snapshot)
CREATE INDEX IF NOT EXISTS idx_course_snapshot_data ON course USING GIN (snapshot_data) WHERE snapshot_data IS NOT NULL;

-- Step 3: Add comment
COMMENT ON COLUMN course.snapshot_data IS 'JSON snapshot of course tree (chapters, lessons, sections, contents) when teacher submits update. Used to display old content to learners while update is pending approval.';

