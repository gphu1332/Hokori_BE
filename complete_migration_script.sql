-- ============================================
-- COMPLETE MIGRATION SCRIPT FOR SQL SERVER
-- Tổng hợp tất cả các migrations từ Flyway
-- Chạy script này trên SQL Server để xem lại schema
-- ============================================
-- Lưu ý: Script này đã được convert từ PostgreSQL sang SQL Server syntax
-- Không ảnh hưởng đến Railway hay project hiện tại
-- ============================================

USE [hokori_db]; -- Thay đổi tên database nếu cần
GO

-- ============================================
-- MIGRATION 1: Change Quiz from lesson_id to section_id
-- ============================================
PRINT 'Migration 1: Change Quiz from lesson_id to section_id';

-- Step 1: Add new column section_id (nullable first)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('quizzes') AND name = 'section_id'
)
BEGIN
    ALTER TABLE quizzes ADD section_id BIGINT NULL;
    PRINT 'Added section_id column to quizzes table';
END
ELSE
BEGIN
    -- Column exists, make sure it's nullable
    ALTER TABLE quizzes ALTER COLUMN section_id BIGINT NULL;
    PRINT 'section_id column already exists, ensured nullable';
END
GO

-- Step 2: Create index for section_id (if not exists)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_quizzes_section_id' AND object_id = OBJECT_ID('quizzes'))
BEGIN
    CREATE INDEX idx_quizzes_section_id ON quizzes(section_id);
    PRINT 'Created index idx_quizzes_section_id';
END
GO

-- Step 3: Migrate existing data (if any)
-- For each quiz, find the first section of its lesson and assign it
UPDATE q
SET q.section_id = (
    SELECT TOP 1 s.id
    FROM sections s
    WHERE s.lesson_id = q.lesson_id
    ORDER BY s.order_index ASC
)
FROM quizzes q
WHERE q.lesson_id IS NOT NULL AND q.section_id IS NULL;
GO

-- Step 3b: Handle lessons with quiz but no sections
-- Auto-create a default section for lessons that have quiz but no sections
INSERT INTO sections (lesson_id, title, order_index, study_type, created_at, updated_at, deleted_flag)
SELECT DISTINCT
    q.lesson_id,
    'Quiz Section' AS title,
    0 AS order_index,
    'GRAMMAR' AS study_type,
    GETDATE() AS created_at,
    GETDATE() AS updated_at,
    0 AS deleted_flag
FROM quizzes q
WHERE q.section_id IS NULL 
  AND q.lesson_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sections s WHERE s.lesson_id = q.lesson_id
  );
GO

-- Step 3c: Now assign quizzes to the newly created sections
UPDATE q
SET q.section_id = (
    SELECT TOP 1 s.id
    FROM sections s
    WHERE s.lesson_id = q.lesson_id
    ORDER BY s.order_index ASC
)
FROM quizzes q
WHERE q.section_id IS NULL AND q.lesson_id IS NOT NULL;
GO

-- Step 4: Add foreign key constraint for section_id (if not exists)
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'fk_quizzes_section' AND parent_object_id = OBJECT_ID('quizzes')
)
BEGIN
    ALTER TABLE quizzes
    ADD CONSTRAINT fk_quizzes_section
    FOREIGN KEY (section_id) REFERENCES sections(id);
    PRINT 'Added foreign key fk_quizzes_section';
END
GO

-- Step 5: Make section_id NOT NULL (after data migration)
-- Only set NOT NULL if all rows have been migrated (no NULL values)
IF NOT EXISTS (SELECT 1 FROM quizzes WHERE section_id IS NULL)
BEGIN
    ALTER TABLE quizzes ALTER COLUMN section_id BIGINT NOT NULL;
    PRINT 'Set section_id to NOT NULL';
END
ELSE
BEGIN
    PRINT 'WARNING: Cannot set section_id NOT NULL - some quizzes still have NULL section_id';
END
GO

-- Step 6: Drop old foreign key constraint and index
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_quizzes_lesson' AND parent_object_id = OBJECT_ID('quizzes'))
BEGIN
    ALTER TABLE quizzes DROP CONSTRAINT fk_quizzes_lesson;
    PRINT 'Dropped old foreign key fk_quizzes_lesson';
END
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_quizzes_lesson_id' AND object_id = OBJECT_ID('quizzes'))
BEGIN
    DROP INDEX idx_quizzes_lesson_id ON quizzes;
    PRINT 'Dropped old index idx_quizzes_lesson_id';
END
GO

-- Step 7: Drop old column lesson_id
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('quizzes') AND name = 'lesson_id')
BEGIN
    ALTER TABLE quizzes DROP COLUMN lesson_id;
    PRINT 'Dropped old column lesson_id';
END
GO

PRINT 'Migration 1 completed';
GO

-- ============================================
-- MIGRATION 2: Add QUIZ to sections_study_type_check constraint
-- ============================================
PRINT 'Migration 2: Add QUIZ to sections_study_type_check constraint';

-- Step 1: Drop existing constraint if it exists
IF EXISTS (
    SELECT 1 FROM sys.check_constraints 
    WHERE name = 'sections_study_type_check' AND parent_object_id = OBJECT_ID('sections')
)
BEGIN
    ALTER TABLE sections DROP CONSTRAINT sections_study_type_check;
    PRINT 'Dropped existing sections_study_type_check constraint';
END
GO

-- Step 2: Add new constraint with QUIZ included
ALTER TABLE sections 
ADD CONSTRAINT sections_study_type_check 
CHECK (study_type IN ('GRAMMAR', 'VOCABULARY', 'KANJI', 'QUIZ'));
PRINT 'Added sections_study_type_check constraint with QUIZ';
GO

-- ============================================
-- MIGRATION 3: Add QUIZ to sections_content_content_format_check constraint
-- ============================================
PRINT 'Migration 3: Add QUIZ to sections_content_content_format_check constraint';

-- Step 1: Check for any invalid data and fix it
DECLARE @invalid_count INT;
SELECT @invalid_count = COUNT(*)
FROM sections_content
WHERE content_format NOT IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ');

IF @invalid_count > 0
BEGIN
    PRINT 'Found ' + CAST(@invalid_count AS VARCHAR(10)) + ' rows with invalid content_format, updating to ASSET';
    UPDATE sections_content
    SET content_format = 'ASSET'
    WHERE content_format NOT IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ');
END
ELSE
BEGIN
    PRINT 'No invalid content_format values found';
END
GO

-- Step 2: Drop existing constraint if it exists
IF EXISTS (
    SELECT 1 FROM sys.check_constraints 
    WHERE name = 'sections_content_content_format_check' AND parent_object_id = OBJECT_ID('sections_content')
)
BEGIN
    ALTER TABLE sections_content DROP CONSTRAINT sections_content_content_format_check;
    PRINT 'Dropped existing sections_content_content_format_check constraint';
END
GO

-- Step 3: Add new constraint with QUIZ included
DECLARE @violation_count INT;
SELECT @violation_count = COUNT(*)
FROM sections_content
WHERE content_format NOT IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ');

IF @violation_count = 0
BEGIN
    ALTER TABLE sections_content 
    ADD CONSTRAINT sections_content_content_format_check 
    CHECK (content_format IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ'));
    PRINT 'Successfully added sections_content_content_format_check constraint';
END
ELSE
BEGIN
    PRINT 'ERROR: Cannot add constraint - ' + CAST(@violation_count AS VARCHAR(10)) + ' rows still violate the constraint';
END
GO

-- ============================================
-- MIGRATION 4: Add CONVERSATION to ai_quotas_service_type_check constraint
-- ============================================
PRINT 'Migration 4: Add CONVERSATION to ai_quotas_service_type_check constraint';

-- Step 1: Drop existing constraint if it exists
IF EXISTS (
    SELECT 1 FROM sys.check_constraints 
    WHERE name = 'ai_quotas_service_type_check' AND parent_object_id = OBJECT_ID('ai_quotas')
)
BEGIN
    ALTER TABLE ai_quotas DROP CONSTRAINT ai_quotas_service_type_check;
    PRINT 'Dropped existing ai_quotas_service_type_check constraint';
END
GO

-- Step 2: Add new constraint with CONVERSATION included (only if it doesn't exist)
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints 
    WHERE name = 'ai_quotas_service_type_check' AND parent_object_id = OBJECT_ID('ai_quotas')
)
BEGIN
    ALTER TABLE ai_quotas 
    ADD CONSTRAINT ai_quotas_service_type_check 
    CHECK (service_type IN ('GRAMMAR', 'KAIWA', 'PRONUN', 'CONVERSATION'));
    PRINT 'Added ai_quotas_service_type_check constraint with CONVERSATION';
END
GO

-- ============================================
-- MIGRATION 5: Create teacher_revenue table
-- ============================================
PRINT 'Migration 5: Create teacher_revenue table';

-- Step 1: Create teacher_revenue table (only if it doesn't exist)
IF NOT EXISTS (
    SELECT 1 FROM sys.tables 
    WHERE name = 'teacher_revenue' AND schema_id = SCHEMA_ID('dbo')
)
BEGIN
    CREATE TABLE teacher_revenue (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        teacher_id BIGINT NOT NULL,
        course_id BIGINT NOT NULL,
        payment_id BIGINT NOT NULL,
        enrollment_id BIGINT NULL,
        
        -- Revenue calculation
        total_amount_cents BIGINT NOT NULL,
        course_price_cents BIGINT NOT NULL,
        teacher_revenue_cents BIGINT NOT NULL, -- 80% of course price
        admin_commission_cents BIGINT NOT NULL, -- 20% of course price
        
        -- Time tracking
        year_month VARCHAR(7) NOT NULL, -- Format: "2025-01"
        paid_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        
        -- Payout tracking
        is_paid BIT NOT NULL DEFAULT 0,
        payout_date DATETIME2 NULL,
        payout_by_user_id BIGINT NULL,
        payout_note NVARCHAR(MAX) NULL,
        
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
    );
    PRINT 'Created teacher_revenue table';
END
ELSE
BEGIN
    PRINT 'teacher_revenue table already exists, skipping creation';
END
GO

-- Step 2: Add foreign key constraints if they don't exist (idempotent)
-- fk_teacher_revenue_teacher
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'fk_teacher_revenue_teacher' AND parent_object_id = OBJECT_ID('teacher_revenue')
)
BEGIN
    ALTER TABLE teacher_revenue 
    ADD CONSTRAINT fk_teacher_revenue_teacher 
    FOREIGN KEY (teacher_id) REFERENCES users(id);
    PRINT 'Added foreign key fk_teacher_revenue_teacher';
END
GO

-- fk_teacher_revenue_course
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'fk_teacher_revenue_course' AND parent_object_id = OBJECT_ID('teacher_revenue')
)
BEGIN
    ALTER TABLE teacher_revenue 
    ADD CONSTRAINT fk_teacher_revenue_course 
    FOREIGN KEY (course_id) REFERENCES course(id);
    PRINT 'Added foreign key fk_teacher_revenue_course';
END
GO

-- fk_teacher_revenue_payment
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'fk_teacher_revenue_payment' AND parent_object_id = OBJECT_ID('teacher_revenue')
)
BEGIN
    ALTER TABLE teacher_revenue 
    ADD CONSTRAINT fk_teacher_revenue_payment 
    FOREIGN KEY (payment_id) REFERENCES payment(id);
    PRINT 'Added foreign key fk_teacher_revenue_payment';
END
GO

-- fk_teacher_revenue_enrollment
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'fk_teacher_revenue_enrollment' AND parent_object_id = OBJECT_ID('teacher_revenue')
)
BEGIN
    ALTER TABLE teacher_revenue 
    ADD CONSTRAINT fk_teacher_revenue_enrollment 
    FOREIGN KEY (enrollment_id) REFERENCES enrollment(id);
    PRINT 'Added foreign key fk_teacher_revenue_enrollment';
END
GO

-- fk_teacher_revenue_payout_by
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'fk_teacher_revenue_payout_by' AND parent_object_id = OBJECT_ID('teacher_revenue')
)
BEGIN
    ALTER TABLE teacher_revenue 
    ADD CONSTRAINT fk_teacher_revenue_payout_by 
    FOREIGN KEY (payout_by_user_id) REFERENCES users(id);
    PRINT 'Added foreign key fk_teacher_revenue_payout_by';
END
GO

-- uk_teacher_revenue_payment_course
IF NOT EXISTS (
    SELECT 1 FROM sys.key_constraints 
    WHERE name = 'uk_teacher_revenue_payment_course' AND parent_object_id = OBJECT_ID('teacher_revenue')
)
BEGIN
    ALTER TABLE teacher_revenue 
    ADD CONSTRAINT uk_teacher_revenue_payment_course 
    UNIQUE (payment_id, course_id);
    PRINT 'Added unique constraint uk_teacher_revenue_payment_course';
END
GO

-- chk_teacher_revenue_amounts
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints 
    WHERE name = 'chk_teacher_revenue_amounts' AND parent_object_id = OBJECT_ID('teacher_revenue')
)
BEGIN
    ALTER TABLE teacher_revenue 
    ADD CONSTRAINT chk_teacher_revenue_amounts 
    CHECK (
        teacher_revenue_cents >= 0 AND 
        admin_commission_cents >= 0 AND
        total_amount_cents >= 0 AND
        course_price_cents >= 0
    );
    PRINT 'Added check constraint chk_teacher_revenue_amounts';
END
GO

-- chk_teacher_revenue_year_month (SQL Server regex equivalent)
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints 
    WHERE name = 'chk_teacher_revenue_year_month' AND parent_object_id = OBJECT_ID('teacher_revenue')
)
BEGIN
    ALTER TABLE teacher_revenue 
    ADD CONSTRAINT chk_teacher_revenue_year_month 
    CHECK (year_month LIKE '[0-9][0-9][0-9][0-9]-[0-9][0-9]');
    PRINT 'Added check constraint chk_teacher_revenue_year_month';
END
GO

-- Step 3: Create indexes for performance (idempotent)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_teacher' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_teacher ON teacher_revenue(teacher_id);
    PRINT 'Created index idx_teacher_revenue_teacher';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_course' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_course ON teacher_revenue(course_id);
    PRINT 'Created index idx_teacher_revenue_course';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_payment' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_payment ON teacher_revenue(payment_id);
    PRINT 'Created index idx_teacher_revenue_payment';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_year_month' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_year_month ON teacher_revenue(year_month);
    PRINT 'Created index idx_teacher_revenue_year_month';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_paid' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_paid ON teacher_revenue(is_paid);
    PRINT 'Created index idx_teacher_revenue_paid';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_teacher_month' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_teacher_month ON teacher_revenue(teacher_id, year_month);
    PRINT 'Created index idx_teacher_revenue_teacher_month';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_teacher_paid' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_teacher_paid ON teacher_revenue(teacher_id, is_paid);
    PRINT 'Created index idx_teacher_revenue_teacher_paid';
END
GO

PRINT 'Migration 5 completed';
GO

-- ============================================
-- MIGRATION 6: Add bank account fields to users table
-- ============================================
PRINT 'Migration 6: Add bank account fields to users table';

-- Step 1: Add bank account fields (nullable, teachers can update later)
-- bank_account_number
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('users') AND name = 'bank_account_number'
)
BEGIN
    ALTER TABLE users ADD bank_account_number VARCHAR(100) NULL;
    PRINT 'Added bank_account_number column';
END
GO

-- bank_account_name
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('users') AND name = 'bank_account_name'
)
BEGIN
    ALTER TABLE users ADD bank_account_name VARCHAR(150) NULL;
    PRINT 'Added bank_account_name column';
END
GO

-- bank_name
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('users') AND name = 'bank_name'
)
BEGIN
    ALTER TABLE users ADD bank_name VARCHAR(150) NULL;
    PRINT 'Added bank_name column';
END
GO

-- bank_branch_name
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('users') AND name = 'bank_branch_name'
)
BEGIN
    ALTER TABLE users ADD bank_branch_name VARCHAR(150) NULL;
    PRINT 'Added bank_branch_name column';
END
GO

-- last_payout_date
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('users') AND name = 'last_payout_date'
)
BEGIN
    ALTER TABLE users ADD last_payout_date DATE NULL;
    PRINT 'Added last_payout_date column';
END
GO

-- wallet_balance (for tracking teacher earnings, default 0)
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('users') AND name = 'wallet_balance'
)
BEGIN
    ALTER TABLE users ADD wallet_balance BIGINT NOT NULL DEFAULT 0;
    PRINT 'Added wallet_balance column';
END
ELSE
BEGIN
    -- Update existing NULL values to 0
    UPDATE users SET wallet_balance = 0 WHERE wallet_balance IS NULL;
    -- Make it NOT NULL if it was nullable
    ALTER TABLE users ALTER COLUMN wallet_balance BIGINT NOT NULL;
    ALTER TABLE users ADD CONSTRAINT DF_users_wallet_balance DEFAULT 0 FOR wallet_balance;
    PRINT 'Updated wallet_balance column to NOT NULL with default 0';
END
GO

PRINT 'Migration 6 completed';
GO

-- ============================================
-- MIGRATION 7: Add pending_update_at to course table
-- ============================================
PRINT 'Migration 7: Add pending_update_at to course table';

-- Step 1: Add pending_update_at column to course table
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('course') AND name = 'pending_update_at'
)
BEGIN
    ALTER TABLE course ADD pending_update_at DATETIME2 NULL;
    PRINT 'Added pending_update_at column to course table';
END
GO

-- Step 2: Update course_status_check constraint to include PENDING_UPDATE
-- Drop existing constraint if exists
IF EXISTS (
    SELECT 1 FROM sys.check_constraints 
    WHERE name = 'course_status_check' AND parent_object_id = OBJECT_ID('course')
)
BEGIN
    ALTER TABLE course DROP CONSTRAINT course_status_check;
    PRINT 'Dropped existing course_status_check constraint';
END
GO

-- Add new constraint with PENDING_UPDATE
ALTER TABLE course 
ADD CONSTRAINT course_status_check 
CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'PUBLISHED', 'PENDING_UPDATE', 'FLAGGED', 'ARCHIVED'));
PRINT 'Added course_status_check constraint with PENDING_UPDATE';
GO

PRINT 'Migration 7 completed';
GO

-- ============================================
-- MIGRATION 8: Add snapshot_data to course table
-- ============================================
PRINT 'Migration 8: Add snapshot_data to course table';

-- Step 1: Add snapshot_data column (NVARCHAR(MAX) for JSON storage in SQL Server)
-- Note: SQL Server 2016+ supports JSON type, but NVARCHAR(MAX) is more compatible
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('course') AND name = 'snapshot_data'
)
BEGIN
    ALTER TABLE course ADD snapshot_data NVARCHAR(MAX) NULL;
    PRINT 'Added snapshot_data column to course table';
END
GO

-- Step 2: Add index for snapshot_data (SQL Server doesn't have GIN index like PostgreSQL)
-- Instead, we can add a computed column index if needed, but for now just add a regular index
-- Note: Full-text index might be better for JSON queries, but keeping it simple here
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_course_snapshot_data' AND object_id = OBJECT_ID('course'))
BEGIN
    -- For SQL Server, we can't index NVARCHAR(MAX) directly, so we skip this
    -- If needed, consider using a computed column or full-text index
    PRINT 'Note: Index on snapshot_data skipped (NVARCHAR(MAX) cannot be indexed directly in SQL Server)';
END
GO

PRINT 'Migration 8 completed';
GO

-- ============================================
-- SUMMARY
-- ============================================
PRINT '';
PRINT '============================================';
PRINT 'ALL MIGRATIONS COMPLETED SUCCESSFULLY';
PRINT '============================================';
PRINT '';
PRINT 'Migrations applied:';
PRINT '  1. Change Quiz from lesson_id to section_id';
PRINT '  2. Add QUIZ to sections_study_type_check';
PRINT '  3. Add QUIZ to sections_content_content_format_check';
PRINT '  4. Add CONVERSATION to ai_quotas_service_type_check';
PRINT '  5. Create teacher_revenue table';
PRINT '  6. Add bank account fields to users table';
PRINT '  7. Add pending_update_at to course table';
PRINT '  8. Add snapshot_data to course table';
PRINT '';
PRINT 'Script execution completed!';
GO

