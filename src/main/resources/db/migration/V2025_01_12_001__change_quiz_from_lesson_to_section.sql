-- Migration: Change Quiz from lesson_id to section_id
-- This migration changes the quiz association from Lesson to Section
-- Railway/Flyway will automatically run this migration on deploy

-- Step 1: Add new column section_id (nullable first)
ALTER TABLE quizzes ADD COLUMN section_id BIGINT NULL;

-- Step 2: Create index for section_id
CREATE INDEX idx_quizzes_section_id ON quizzes(section_id);

-- Step 3: Migrate existing data (if any)
-- For each quiz, find the first section of its lesson and assign it
UPDATE quizzes q
SET section_id = (
    SELECT s.id
    FROM sections s
    WHERE s.lesson_id = q.lesson_id
    ORDER BY s.order_index ASC
    LIMIT 1
)
WHERE q.lesson_id IS NOT NULL;

-- Step 3b: Handle lessons with quiz but no sections
-- Auto-create a default section for lessons that have quiz but no sections
-- This preserves quiz data for old courses
-- Note: study_type = 'GRAMMAR' is default (quiz is a feature, not a study type)
INSERT INTO sections (lesson_id, title, order_index, study_type, created_at, updated_at, deleted_flag)
SELECT DISTINCT
    q.lesson_id,
    'Quiz Section' AS title,
    0 AS order_index,
    'GRAMMAR' AS study_type,  -- Default study type (quiz is a feature of section, not a study type)
    NOW() AS created_at,
    NOW() AS updated_at,
    false AS deleted_flag
FROM quizzes q
WHERE q.section_id IS NULL 
  AND q.lesson_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sections s WHERE s.lesson_id = q.lesson_id
  );

-- Step 3c: Now assign quizzes to the newly created sections
UPDATE quizzes q
SET section_id = (
    SELECT s.id
    FROM sections s
    WHERE s.lesson_id = q.lesson_id
    ORDER BY s.order_index ASC
    LIMIT 1
)
WHERE q.section_id IS NULL AND q.lesson_id IS NOT NULL;

-- Step 3d: Safety check - if still NULL, something went wrong
-- Log warning but don't fail migration (quizzes will be orphaned but not deleted)
-- In production, you should check logs after migration
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM quizzes WHERE section_id IS NULL AND lesson_id IS NOT NULL) THEN
        RAISE WARNING 'Some quizzes could not be migrated to sections. Check quizzes table.';
    END IF;
END $$;

-- Step 4: Add foreign key constraint for section_id
ALTER TABLE quizzes
ADD CONSTRAINT fk_quizzes_section
FOREIGN KEY (section_id) REFERENCES sections(id);

-- Step 5: Make section_id NOT NULL (after data migration)
ALTER TABLE quizzes ALTER COLUMN section_id SET NOT NULL;

-- Step 6: Drop old foreign key constraint and index
ALTER TABLE quizzes DROP CONSTRAINT IF EXISTS fk_quizzes_lesson;
DROP INDEX IF EXISTS idx_quizzes_lesson_id;

-- Step 7: Drop old column lesson_id
ALTER TABLE quizzes DROP COLUMN lesson_id;

