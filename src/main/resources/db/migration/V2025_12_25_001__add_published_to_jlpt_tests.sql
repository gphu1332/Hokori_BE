-- Add published column to jlpt_tests table
-- Test will only be visible to learners when published = true
-- Default to false for existing tests (they need to be republished if needed)

ALTER TABLE jlpt_tests
ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE;

-- Set all existing tests to published = true (assuming they are already complete)
-- If you want to review existing tests, comment out this line
UPDATE jlpt_tests SET published = TRUE WHERE deleted_flag = FALSE;

-- Add comment
COMMENT ON COLUMN jlpt_tests.published IS 'Whether the test is published and visible to learners. Only published tests with all 4 skills (LISTENING, READING, GRAMMAR, VOCAB) should be visible.';

