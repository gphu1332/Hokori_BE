-- ============================================
-- ADD STRUCTURED REJECTION REASONS TO COURSE
-- ============================================
-- Thay thế cách lưu rejection_reason bằng JSON string
-- Bằng cách tạo các field riêng cho từng phần và bảng riêng cho chapters/lessons/sections
-- ============================================

-- Step 1: Add rejection reason fields for course main parts
DO $$
BEGIN
    -- Add rejection_reason_general (backward compatible với rejection_reason cũ)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course' 
        AND column_name = 'rejection_reason_general'
    ) THEN
        ALTER TABLE course ADD COLUMN rejection_reason_general TEXT NULL;
        RAISE NOTICE 'Added column: rejection_reason_general';
    END IF;

    -- Add rejection_reason_title
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course' 
        AND column_name = 'rejection_reason_title'
    ) THEN
        ALTER TABLE course ADD COLUMN rejection_reason_title TEXT NULL;
        RAISE NOTICE 'Added column: rejection_reason_title';
    END IF;

    -- Add rejection_reason_subtitle
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course' 
        AND column_name = 'rejection_reason_subtitle'
    ) THEN
        ALTER TABLE course ADD COLUMN rejection_reason_subtitle TEXT NULL;
        RAISE NOTICE 'Added column: rejection_reason_subtitle';
    END IF;

    -- Add rejection_reason_description
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course' 
        AND column_name = 'rejection_reason_description'
    ) THEN
        ALTER TABLE course ADD COLUMN rejection_reason_description TEXT NULL;
        RAISE NOTICE 'Added column: rejection_reason_description';
    END IF;

    -- Add rejection_reason_cover_image
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course' 
        AND column_name = 'rejection_reason_cover_image'
    ) THEN
        ALTER TABLE course ADD COLUMN rejection_reason_cover_image TEXT NULL;
        RAISE NOTICE 'Added column: rejection_reason_cover_image';
    END IF;

    -- Add rejection_reason_price
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'course' 
        AND column_name = 'rejection_reason_price'
    ) THEN
        ALTER TABLE course ADD COLUMN rejection_reason_price TEXT NULL;
        RAISE NOTICE 'Added column: rejection_reason_price';
    END IF;
END $$;

-- Step 2: Migrate existing rejection_reason to rejection_reason_general (backward compatibility)
DO $$
BEGIN
    UPDATE course 
    SET rejection_reason_general = rejection_reason 
    WHERE rejection_reason IS NOT NULL 
    AND rejection_reason_general IS NULL;
    RAISE NOTICE 'Migrated existing rejection_reason to rejection_reason_general';
END $$;

-- Step 3: Create course_rejection_reason_detail table for chapters/lessons/sections
CREATE TABLE IF NOT EXISTS course_rejection_reason_detail (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    item_type VARCHAR(20) NOT NULL CHECK (item_type IN ('CHAPTER', 'LESSON', 'SECTION')),
    item_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rejected_by_user_id BIGINT NULL,
    
    CONSTRAINT fk_course_rejection_reason_detail_course 
        FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_course_rejection_reason_detail_user 
        FOREIGN KEY (rejected_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Ensure one reason per item per course rejection
    CONSTRAINT uk_course_rejection_reason_detail 
        UNIQUE (course_id, item_type, item_id)
);

-- Step 4: Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_course_rejection_reason_detail_course_id 
    ON course_rejection_reason_detail(course_id);
CREATE INDEX IF NOT EXISTS idx_course_rejection_reason_detail_item 
    ON course_rejection_reason_detail(item_type, item_id);

-- Step 5: Add comments
COMMENT ON COLUMN course.rejection_reason_general IS 'General rejection reason for the entire course (backward compatible with old rejection_reason)';
COMMENT ON COLUMN course.rejection_reason_title IS 'Rejection reason specifically for course title';
COMMENT ON COLUMN course.rejection_reason_subtitle IS 'Rejection reason specifically for course subtitle';
COMMENT ON COLUMN course.rejection_reason_description IS 'Rejection reason specifically for course description';
COMMENT ON COLUMN course.rejection_reason_cover_image IS 'Rejection reason specifically for cover image';
COMMENT ON COLUMN course.rejection_reason_price IS 'Rejection reason specifically for pricing';
COMMENT ON TABLE course_rejection_reason_detail IS 'Detailed rejection reasons for specific chapters, lessons, or sections within a course';

