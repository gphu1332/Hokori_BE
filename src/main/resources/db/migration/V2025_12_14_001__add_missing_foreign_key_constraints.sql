-- ============================================
-- Migration: Add Missing Foreign Key Constraints
-- ============================================
-- This migration ensures all foreign key constraints exist in the database
-- It's idempotent - will only add constraints if they don't already exist
-- This fixes inconsistencies between entities (@ManyToOne) and database schema
-- ============================================

-- ============================================
-- ENROLLMENT TABLE
-- ============================================
-- Add fk_enrollment_user if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_enrollment_user' 
        AND conrelid = 'enrollment'::regclass
    ) THEN
        ALTER TABLE enrollment 
        ADD CONSTRAINT fk_enrollment_user 
        FOREIGN KEY (user_id) REFERENCES users(id);
        RAISE NOTICE 'Added constraint: fk_enrollment_user';
    END IF;
END $$;

-- Add fk_enrollment_course if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_enrollment_course' 
        AND conrelid = 'enrollment'::regclass
    ) THEN
        ALTER TABLE enrollment 
        ADD CONSTRAINT fk_enrollment_course 
        FOREIGN KEY (course_id) REFERENCES course(id);
        RAISE NOTICE 'Added constraint: fk_enrollment_course';
    END IF;
END $$;

-- ============================================
-- PAYMENT TABLE
-- ============================================
-- Add fk_payment_user if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_payment_user' 
        AND conrelid = 'payment'::regclass
    ) THEN
        ALTER TABLE payment 
        ADD CONSTRAINT fk_payment_user 
        FOREIGN KEY (user_id) REFERENCES users(id);
        RAISE NOTICE 'Added constraint: fk_payment_user';
    END IF;
END $$;

-- ============================================
-- QUIZ_ATTEMPTS TABLE
-- ============================================
-- Add fk_quiz_attempt_user if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_quiz_attempt_user' 
        AND conrelid = 'quiz_attempts'::regclass
    ) THEN
        ALTER TABLE quiz_attempts 
        ADD CONSTRAINT fk_quiz_attempt_user 
        FOREIGN KEY (user_id) REFERENCES users(id);
        RAISE NOTICE 'Added constraint: fk_quiz_attempt_user';
    END IF;
END $$;

-- Add fk_quiz_attempt_quiz if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_quiz_attempt_quiz' 
        AND conrelid = 'quiz_attempts'::regclass
    ) THEN
        ALTER TABLE quiz_attempts 
        ADD CONSTRAINT fk_quiz_attempt_quiz 
        FOREIGN KEY (quiz_id) REFERENCES quizzes(id);
        RAISE NOTICE 'Added constraint: fk_quiz_attempt_quiz';
    END IF;
END $$;

-- ============================================
-- COURSE_FLAG TABLE
-- ============================================
-- Add fk_course_flag_user if not exists (course FK already exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_course_flag_user' 
        AND conrelid = 'course_flag'::regclass
    ) THEN
        ALTER TABLE course_flag 
        ADD CONSTRAINT fk_course_flag_user 
        FOREIGN KEY (user_id) REFERENCES users(id);
        RAISE NOTICE 'Added constraint: fk_course_flag_user';
    END IF;
END $$;

-- ============================================
-- NOTIFICATIONS TABLE
-- ============================================
-- Add fk_notification_user if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_notification_user' 
        AND conrelid = 'notifications'::regclass
    ) THEN
        ALTER TABLE notifications 
        ADD CONSTRAINT fk_notification_user 
        FOREIGN KEY (user_id) REFERENCES users(id);
        RAISE NOTICE 'Added constraint: fk_notification_user';
    END IF;
END $$;

-- ============================================
-- COURSE_COMPLETION_CERTIFICATES TABLE
-- ============================================
-- Add fk_cc_user if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_cc_user' 
        AND conrelid = 'course_completion_certificates'::regclass
    ) THEN
        ALTER TABLE course_completion_certificates 
        ADD CONSTRAINT fk_cc_user 
        FOREIGN KEY (user_id) REFERENCES users(id);
        RAISE NOTICE 'Added constraint: fk_cc_user';
    END IF;
END $$;

-- Add fk_cc_course if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_cc_course' 
        AND conrelid = 'course_completion_certificates'::regclass
    ) THEN
        ALTER TABLE course_completion_certificates 
        ADD CONSTRAINT fk_cc_course 
        FOREIGN KEY (course_id) REFERENCES course(id);
        RAISE NOTICE 'Added constraint: fk_cc_course';
    END IF;
END $$;

-- Add fk_cc_enrollment if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_cc_enrollment' 
        AND conrelid = 'course_completion_certificates'::regclass
    ) THEN
        ALTER TABLE course_completion_certificates 
        ADD CONSTRAINT fk_cc_enrollment 
        FOREIGN KEY (enrollment_id) REFERENCES enrollment(id);
        RAISE NOTICE 'Added constraint: fk_cc_enrollment';
    END IF;
END $$;

-- ============================================
-- USER_DAILY_LEARNING TABLE
-- ============================================
-- Add fk_user_daily_learning_user if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_user_daily_learning_user' 
        AND conrelid = 'user_daily_learning'::regclass
    ) THEN
        ALTER TABLE user_daily_learning 
        ADD CONSTRAINT fk_user_daily_learning_user 
        FOREIGN KEY (user_id) REFERENCES users(id);
        RAISE NOTICE 'Added constraint: fk_user_daily_learning_user';
    END IF;
END $$;

-- ============================================
-- MIGRATION COMPLETED
-- ============================================
-- All foreign key constraints have been added (idempotent)
-- Entities with @ManyToOne relationships now match database schema
-- ============================================

