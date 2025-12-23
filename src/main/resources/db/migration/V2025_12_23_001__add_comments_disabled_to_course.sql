-- ============================================
-- ADD COMMENTS_DISABLED COLUMN TO COURSE TABLE
-- ============================================
-- Thêm column để moderator có thể disable comments cho course
-- Default: FALSE (comments enabled)
-- ============================================

-- Add comments_disabled column if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course' 
        AND column_name = 'comments_disabled'
    ) THEN
        ALTER TABLE course 
        ADD COLUMN comments_disabled BOOLEAN NOT NULL DEFAULT FALSE;
        
        RAISE NOTICE 'Added column: comments_disabled to course table';
    ELSE
        RAISE NOTICE 'Column comments_disabled already exists in course table';
    END IF;
END $$;

-- Add index for comments_disabled (for query optimization)
CREATE INDEX IF NOT EXISTS idx_course_comments_disabled ON course(comments_disabled);

COMMENT ON COLUMN course.comments_disabled IS 'Moderator can disable comments for problematic courses';

