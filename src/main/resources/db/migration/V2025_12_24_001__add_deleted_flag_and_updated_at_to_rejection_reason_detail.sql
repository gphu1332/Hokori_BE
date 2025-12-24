-- ============================================
-- ADD DELETED_FLAG AND UPDATED_AT TO COURSE_REJECTION_REASON_DETAIL
-- ============================================
-- Fix missing columns from BaseEntity: deleted_flag and updated_at

-- Add deleted_flag column
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course_rejection_reason_detail' 
        AND column_name = 'deleted_flag'
    ) THEN
        ALTER TABLE course_rejection_reason_detail 
        ADD COLUMN deleted_flag BOOLEAN NOT NULL DEFAULT FALSE;
        RAISE NOTICE 'Added column: deleted_flag to course_rejection_reason_detail';
    END IF;
END $$;

-- Add updated_at column
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course_rejection_reason_detail' 
        AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE course_rejection_reason_detail 
        ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE 'Added column: updated_at to course_rejection_reason_detail';
    END IF;
END $$;

-- Update existing records to set updated_at = created_at if null
DO $$
BEGIN
    UPDATE course_rejection_reason_detail 
    SET updated_at = created_at 
    WHERE updated_at IS NULL;
    RAISE NOTICE 'Updated existing records: set updated_at = created_at';
END $$;

-- Add index for deleted_flag for performance
CREATE INDEX IF NOT EXISTS idx_course_rejection_reason_detail_deleted_flag 
    ON course_rejection_reason_detail(deleted_flag);

-- Add comments
COMMENT ON COLUMN course_rejection_reason_detail.deleted_flag IS 'Soft delete flag (inherited from BaseEntity)';
COMMENT ON COLUMN course_rejection_reason_detail.updated_at IS 'Last update timestamp (inherited from BaseEntity)';

