-- Migration: Create teacher_revenue table for tracking revenue and payouts
-- This table tracks revenue from course sales, split between teacher (80%) and admin (20%)

-- Step 1: Create teacher_revenue table (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name = 'teacher_revenue'
    ) THEN
        CREATE TABLE teacher_revenue (
            id BIGSERIAL PRIMARY KEY,
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
            paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            
            -- Payout tracking
            is_paid BOOLEAN NOT NULL DEFAULT FALSE,
            payout_date TIMESTAMP NULL,
            payout_by_user_id BIGINT NULL,
            payout_note TEXT NULL,
            
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        RAISE NOTICE 'Created teacher_revenue table';
    ELSE
        RAISE NOTICE 'teacher_revenue table already exists, skipping creation';
    END IF;
END $$;

-- Step 2: Add foreign key constraints if they don't exist (idempotent)
DO $$
BEGIN
    -- fk_teacher_revenue_teacher
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_teacher_revenue_teacher'
    ) THEN
        ALTER TABLE teacher_revenue 
        ADD CONSTRAINT fk_teacher_revenue_teacher 
        FOREIGN KEY (teacher_id) REFERENCES users(id);
    END IF;
    
    -- fk_teacher_revenue_course
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_teacher_revenue_course'
    ) THEN
        ALTER TABLE teacher_revenue 
        ADD CONSTRAINT fk_teacher_revenue_course 
        FOREIGN KEY (course_id) REFERENCES course(id);
    END IF;
    
    -- fk_teacher_revenue_payment
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_teacher_revenue_payment'
    ) THEN
        ALTER TABLE teacher_revenue 
        ADD CONSTRAINT fk_teacher_revenue_payment 
        FOREIGN KEY (payment_id) REFERENCES payment(id);
    END IF;
    
    -- fk_teacher_revenue_enrollment
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_teacher_revenue_enrollment'
    ) THEN
        ALTER TABLE teacher_revenue 
        ADD CONSTRAINT fk_teacher_revenue_enrollment 
        FOREIGN KEY (enrollment_id) REFERENCES enrollment(id);
    END IF;
    
    -- fk_teacher_revenue_payout_by
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_teacher_revenue_payout_by'
    ) THEN
        ALTER TABLE teacher_revenue 
        ADD CONSTRAINT fk_teacher_revenue_payout_by 
        FOREIGN KEY (payout_by_user_id) REFERENCES users(id);
    END IF;
    
    -- uk_teacher_revenue_payment_course
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_teacher_revenue_payment_course'
    ) THEN
        ALTER TABLE teacher_revenue 
        ADD CONSTRAINT uk_teacher_revenue_payment_course 
        UNIQUE (payment_id, course_id);
    END IF;
    
    -- chk_teacher_revenue_amounts
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_teacher_revenue_amounts'
    ) THEN
        ALTER TABLE teacher_revenue 
        ADD CONSTRAINT chk_teacher_revenue_amounts 
        CHECK (
            teacher_revenue_cents >= 0 AND 
            admin_commission_cents >= 0 AND
            total_amount_cents >= 0 AND
            course_price_cents >= 0
        );
    END IF;
    
    -- chk_teacher_revenue_year_month
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_teacher_revenue_year_month'
    ) THEN
        ALTER TABLE teacher_revenue 
        ADD CONSTRAINT chk_teacher_revenue_year_month 
        CHECK (year_month ~ '^\d{4}-\d{2}$');
    END IF;
END $$;

-- Step 3: Create indexes for performance (idempotent)
CREATE INDEX IF NOT EXISTS idx_teacher_revenue_teacher ON teacher_revenue(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_revenue_course ON teacher_revenue(course_id);
CREATE INDEX IF NOT EXISTS idx_teacher_revenue_payment ON teacher_revenue(payment_id);
CREATE INDEX IF NOT EXISTS idx_teacher_revenue_year_month ON teacher_revenue(year_month);
CREATE INDEX IF NOT EXISTS idx_teacher_revenue_paid ON teacher_revenue(is_paid);
CREATE INDEX IF NOT EXISTS idx_teacher_revenue_teacher_month ON teacher_revenue(teacher_id, year_month);
CREATE INDEX IF NOT EXISTS idx_teacher_revenue_teacher_paid ON teacher_revenue(teacher_id, is_paid);

-- Step 4: Add comments
COMMENT ON TABLE teacher_revenue IS 'Tracks revenue from course sales, split between teacher (80%) and admin (20%)';
COMMENT ON COLUMN teacher_revenue.teacher_revenue_cents IS '80% of course price - amount teacher receives';
COMMENT ON COLUMN teacher_revenue.admin_commission_cents IS '20% of course price - admin commission';
COMMENT ON COLUMN teacher_revenue.year_month IS 'Month when payment was made (format: YYYY-MM)';
COMMENT ON COLUMN teacher_revenue.is_paid IS 'Whether admin has marked this revenue as paid to teacher';
COMMENT ON COLUMN teacher_revenue.payout_date IS 'Date when admin marked this revenue as paid';

