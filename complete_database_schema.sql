-- ============================================
-- COMPLETE DATABASE SCHEMA FOR SQL SERVER
-- Tạo toàn bộ database từ đầu bao gồm:
-- - Tất cả tables
-- - Tất cả constraints (foreign keys, check constraints, unique constraints)
-- - Tất cả indexes
-- - Tất cả migrations đã áp dụng
-- ============================================
-- Lưu ý: Script này đã được convert từ PostgreSQL sang SQL Server syntax
-- Không ảnh hưởng đến Railway hay project hiện tại
-- Chạy script này trên SQL Server để tạo database hoàn chỉnh
-- ============================================

USE [master];
GO

-- Tạo database nếu chưa có (uncomment nếu cần)
-- IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'hokori_db')
-- BEGIN
--     CREATE DATABASE [hokori_db];
-- END
-- GO

USE [hokori_db]; -- Thay đổi tên database nếu cần
GO

-- ============================================
-- PART 1: CREATE BASE TABLES (Core Tables)
-- ============================================

-- Table: roles
PRINT 'Creating table: roles';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'roles' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE roles (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        role_name VARCHAR(50) NOT NULL UNIQUE,
        description VARCHAR(255) NULL,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL
    );
    PRINT 'Created table: roles';
END
GO

-- Table: users
PRINT 'Creating table: users';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'users' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE users (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        firebase_uid VARCHAR(128) NULL,
        firebase_provider VARCHAR(50) NULL,
        firebase_email_verified BIT NULL,
        email VARCHAR(255) NOT NULL,
        username VARCHAR(100) NULL,
        password_hash VARCHAR(255) NULL,
        is_active BIT NOT NULL DEFAULT 1,
        is_verified BIT NOT NULL DEFAULT 0,
        display_name VARCHAR(255) NULL,
        first_name VARCHAR(100) NULL,
        last_name VARCHAR(100) NULL,
        avatar_url VARCHAR(500) NULL,
        banner_url VARCHAR(500) NULL,
        phone_number VARCHAR(20) NULL,
        date_of_birth DATE NULL,
        gender VARCHAR(10) NULL DEFAULT 'OTHER',
        address VARCHAR(255) NULL,
        current_jlpt_level VARCHAR(10) NULL DEFAULT 'N5',
        bio NVARCHAR(MAX) NULL,
        years_of_experience INT NULL,
        website_url VARCHAR(255) NULL,
        linkedin VARCHAR(255) NULL,
        approval_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
        approved_at DATETIME2 NULL,
        approved_by_user_id BIGINT NULL,
        profile_approval_request_id BIGINT NULL,
        -- Bank account fields (from migration)
        bank_account_number VARCHAR(100) NULL,
        bank_account_name VARCHAR(150) NULL,
        bank_name VARCHAR(150) NULL,
        bank_branch_name VARCHAR(150) NULL,
        last_payout_date DATE NULL,
        wallet_balance BIGINT NOT NULL DEFAULT 0,
        last_login_at DATETIME2 NULL,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        deleted_flag BIT NOT NULL DEFAULT 0,
        role_id BIGINT NULL,
        
        CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
    );
    PRINT 'Created table: users';
END
GO

-- Indexes for users
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_users_email' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE UNIQUE INDEX idx_users_email ON users(email);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_users_username' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE INDEX idx_users_username ON users(username);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_users_firebase_uid' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE INDEX idx_users_firebase_uid ON users(firebase_uid);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_users_role_id' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE INDEX idx_users_role_id ON users(role_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_users_approval_status' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE INDEX idx_users_approval_status ON users(approval_status);
END
GO

-- Table: course
PRINT 'Creating table: course';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'course' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE course (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        cover_image_path VARCHAR(500) NULL,
        title NVARCHAR(255) NOT NULL,
        slug VARCHAR(180) NOT NULL UNIQUE,
        subtitle NVARCHAR(255) NULL,
        description NVARCHAR(MAX) NULL,
        level VARCHAR(10) NOT NULL DEFAULT 'N5',
        price_cents BIGINT NULL,
        discounted_price_cents BIGINT NULL,
        currency VARCHAR(10) NULL DEFAULT 'VND',
        user_id BIGINT NOT NULL,
        status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
        published_at DATETIME2 NULL,
        -- Pending update fields (from migration)
        pending_update_at DATETIME2 NULL,
        snapshot_data NVARCHAR(MAX) NULL, -- JSON data
        rejection_reason NVARCHAR(MAX) NULL,
        rejected_at DATETIME2 NULL,
        rejected_by_user_id BIGINT NULL,
        flagged_reason NVARCHAR(MAX) NULL,
        flagged_at DATETIME2 NULL,
        flagged_by_user_id BIGINT NULL,
        rating_avg FLOAT NULL DEFAULT 0.0,
        rating_count BIGINT NULL DEFAULT 0,
        enroll_count BIGINT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_course_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT course_status_check CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'PUBLISHED', 'PENDING_UPDATE', 'FLAGGED', 'ARCHIVED'))
    );
    PRINT 'Created table: course';
END
GO

-- Table: chapter
PRINT 'Creating table: chapter';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'chapter' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE chapter (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        course_id BIGINT NOT NULL,
        title NVARCHAR(255) NOT NULL,
        order_index INT NOT NULL DEFAULT 0,
        summary NVARCHAR(MAX) NULL,
        is_trial BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_chapter_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
    );
    PRINT 'Created table: chapter';
END
GO

-- Table: lessons
PRINT 'Creating table: lessons';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'lessons' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE lessons (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        chapter_id BIGINT NOT NULL,
        title NVARCHAR(255) NOT NULL,
        order_index INT NOT NULL DEFAULT 0,
        total_duration_sec BIGINT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_lessons_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE
    );
    PRINT 'Created table: lessons';
END
GO

-- Table: sections
PRINT 'Creating table: sections';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'sections' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE sections (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        lesson_id BIGINT NOT NULL,
        title NVARCHAR(255) NOT NULL,
        order_index INT NOT NULL DEFAULT 0,
        study_type VARCHAR(50) NOT NULL DEFAULT 'GRAMMAR',
        flashcard_set_id BIGINT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_sections_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
        CONSTRAINT sections_study_type_check CHECK (study_type IN ('GRAMMAR', 'VOCABULARY', 'KANJI', 'QUIZ'))
    );
    PRINT 'Created table: sections';
END
GO

-- Table: sections_content
PRINT 'Creating table: sections_content';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'sections_content' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE sections_content (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        sections_id BIGINT NOT NULL,
        order_index INT NOT NULL DEFAULT 0,
        content_format VARCHAR(50) NOT NULL DEFAULT 'ASSET',
        primary_content BIT NOT NULL DEFAULT 0,
        file_path VARCHAR(500) NULL,
        rich_text NVARCHAR(MAX) NULL,
        flashcard_set_id BIGINT NULL,
        quiz_id BIGINT NULL,
        is_trackable BIT NOT NULL DEFAULT 1,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_sections_content_section FOREIGN KEY (sections_id) REFERENCES sections(id) ON DELETE CASCADE,
        CONSTRAINT sections_content_content_format_check CHECK (content_format IN ('ASSET', 'RICH_TEXT', 'FLASHCARD_SET', 'QUIZ'))
    );
    PRINT 'Created table: sections_content';
END
GO

-- Table: quizzes
PRINT 'Creating table: quizzes';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'quizzes' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE quizzes (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        section_id BIGINT NOT NULL, -- Changed from lesson_id (migration)
        title NVARCHAR(255) NOT NULL,
        description NVARCHAR(MAX) NULL,
        total_questions INT NOT NULL DEFAULT 0,
        time_limit_sec INT NULL,
        pass_score_percent INT NULL,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        deleted_flag BIT NULL DEFAULT 0,
        
        CONSTRAINT fk_quizzes_section FOREIGN KEY (section_id) REFERENCES sections(id)
    );
    PRINT 'Created table: quizzes';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_quizzes_section_id' AND object_id = OBJECT_ID('quizzes'))
BEGIN
    CREATE INDEX idx_quizzes_section_id ON quizzes(section_id);
END
GO

-- Table: enrollment
PRINT 'Creating table: enrollment';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'enrollment' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE enrollment (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        course_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        progress_percent INT NOT NULL DEFAULT 0,
        started_at DATETIME2 NULL,
        completed_at DATETIME2 NULL,
        last_access_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES course(id),
        CONSTRAINT fk_enrollment_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT uk_enroll_user_course UNIQUE (user_id, course_id)
    );
    PRINT 'Created table: enrollment';
END
GO

-- Table: user_content_progress
PRINT 'Creating table: user_content_progress';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'user_content_progress' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE user_content_progress (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        enrollment_id BIGINT NOT NULL,
        content_id BIGINT NOT NULL,
        last_position_sec BIGINT NOT NULL DEFAULT 0,
        is_completed BIT NOT NULL DEFAULT 0,
        completed_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_ucp_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
        CONSTRAINT fk_ucp_content FOREIGN KEY (content_id) REFERENCES sections_content(id),
        CONSTRAINT uk_ucp_enrollment_content UNIQUE (enrollment_id, content_id)
    );
    PRINT 'Created table: user_content_progress';
END
GO

-- Table: cart
PRINT 'Creating table: cart';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'cart' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE cart (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT uk_cart_user_open UNIQUE (user_id)
    );
    PRINT 'Created table: cart';
END
GO

-- Table: cartitem
PRINT 'Creating table: cartitem';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'cartitem' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE cartitem (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        cart_id BIGINT NOT NULL,
        course_id BIGINT NOT NULL,
        quantity INT NOT NULL DEFAULT 1,
        total_price BIGINT NOT NULL,
        is_selected BIT NOT NULL DEFAULT 1,
        added_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_cartitem_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
        CONSTRAINT fk_cartitem_course FOREIGN KEY (course_id) REFERENCES course(id),
        CONSTRAINT uk_cart_course UNIQUE (cart_id, course_id)
    );
    PRINT 'Created table: cartitem';
END
GO

-- Table: payment
PRINT 'Creating table: payment';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'payment' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE payment (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        order_code BIGINT NOT NULL UNIQUE,
        amount_cents BIGINT NOT NULL,
        description VARCHAR(500) NULL,
        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        user_id BIGINT NOT NULL,
        cart_id BIGINT NULL,
        course_ids NVARCHAR(MAX) NULL, -- JSON array
        ai_package_id BIGINT NULL,
        ai_package_purchase_id BIGINT NULL,
        payment_link VARCHAR(1000) NULL,
        payos_transaction_code VARCHAR(255) NULL,
        payos_qr_code VARCHAR(1000) NULL,
        webhook_data NVARCHAR(MAX) NULL,
        expired_at DATETIME2 NULL,
        paid_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id)
    );
    PRINT 'Created table: payment';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_payment_user' AND object_id = OBJECT_ID('payment'))
BEGIN
    CREATE INDEX idx_payment_user ON payment(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_payment_order_code' AND object_id = OBJECT_ID('payment'))
BEGIN
    CREATE UNIQUE INDEX idx_payment_order_code ON payment(order_code);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_payment_status' AND object_id = OBJECT_ID('payment'))
BEGIN
    CREATE INDEX idx_payment_status ON payment(status);
END
GO

-- Table: teacher_revenue (from migration)
PRINT 'Creating table: teacher_revenue';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'teacher_revenue' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE teacher_revenue (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        teacher_id BIGINT NOT NULL,
        course_id BIGINT NOT NULL,
        payment_id BIGINT NOT NULL,
        enrollment_id BIGINT NULL,
        total_amount_cents BIGINT NOT NULL,
        course_price_cents BIGINT NOT NULL,
        teacher_revenue_cents BIGINT NOT NULL,
        admin_commission_cents BIGINT NOT NULL,
        year_month VARCHAR(7) NOT NULL,
        paid_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        is_paid BIT NOT NULL DEFAULT 0,
        payout_date DATETIME2 NULL,
        payout_by_user_id BIGINT NULL,
        payout_note NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT fk_teacher_revenue_teacher FOREIGN KEY (teacher_id) REFERENCES users(id),
        CONSTRAINT fk_teacher_revenue_course FOREIGN KEY (course_id) REFERENCES course(id),
        CONSTRAINT fk_teacher_revenue_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
        CONSTRAINT fk_teacher_revenue_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollment(id),
        CONSTRAINT fk_teacher_revenue_payout_by FOREIGN KEY (payout_by_user_id) REFERENCES users(id),
        CONSTRAINT uk_teacher_revenue_payment_course UNIQUE (payment_id, course_id),
        CONSTRAINT chk_teacher_revenue_amounts CHECK (
            teacher_revenue_cents >= 0 AND 
            admin_commission_cents >= 0 AND
            total_amount_cents >= 0 AND
            course_price_cents >= 0
        ),
        CONSTRAINT chk_teacher_revenue_year_month CHECK (year_month LIKE '[0-9][0-9][0-9][0-9]-[0-9][0-9]')
    );
    PRINT 'Created table: teacher_revenue';
END
GO

-- Indexes for teacher_revenue
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_teacher' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_teacher ON teacher_revenue(teacher_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_course' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_course ON teacher_revenue(course_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_payment' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_payment ON teacher_revenue(payment_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_year_month' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_year_month ON teacher_revenue(year_month);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_paid' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_paid ON teacher_revenue(is_paid);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_teacher_month' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_teacher_month ON teacher_revenue(teacher_id, year_month);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_teacher_revenue_teacher_paid' AND object_id = OBJECT_ID('teacher_revenue'))
BEGIN
    CREATE INDEX idx_teacher_revenue_teacher_paid ON teacher_revenue(teacher_id, is_paid);
END
GO

-- Table: wallet_transactions
PRINT 'Creating table: wallet_transactions';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'wallet_transactions' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE wallet_transactions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        status VARCHAR(30) NOT NULL,
        amount_cents BIGINT NOT NULL,
        balance_after_cents BIGINT NOT NULL,
        source VARCHAR(50) NOT NULL,
        course_id BIGINT NULL,
        description VARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        created_by BIGINT NULL,
        
        CONSTRAINT fk_wallet_tx_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_wallet_tx_course FOREIGN KEY (course_id) REFERENCES course(id)
    );
    PRINT 'Created table: wallet_transactions';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_wallet_tx_user' AND object_id = OBJECT_ID('wallet_transactions'))
BEGIN
    CREATE INDEX idx_wallet_tx_user ON wallet_transactions(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_wallet_tx_course' AND object_id = OBJECT_ID('wallet_transactions'))
BEGIN
    CREATE INDEX idx_wallet_tx_course ON wallet_transactions(course_id);
END
GO

-- Table: course_comment
PRINT 'Creating table: course_comment';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'course_comment' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE course_comment (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        course_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        parent_id BIGINT NULL,
        content NVARCHAR(2000) NOT NULL,
        is_edited BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_cc_course FOREIGN KEY (course_id) REFERENCES course(id),
        CONSTRAINT fk_cc_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_cc_parent FOREIGN KEY (parent_id) REFERENCES course_comment(id)
    );
    PRINT 'Created table: course_comment';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_cc_course' AND object_id = OBJECT_ID('course_comment'))
BEGIN
    CREATE INDEX ix_cc_course ON course_comment(course_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_cc_course_parent' AND object_id = OBJECT_ID('course_comment'))
BEGIN
    CREATE INDEX ix_cc_course_parent ON course_comment(course_id, parent_id, created_at);
END
GO

-- Table: course_flag
PRINT 'Creating table: course_flag';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'course_flag' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE course_flag (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        course_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        flag_type VARCHAR(50) NOT NULL,
        reason NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_course_flag_course FOREIGN KEY (course_id) REFERENCES course(id)
    );
    PRINT 'Created table: course_flag';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_course_flag_course_id' AND object_id = OBJECT_ID('course_flag'))
BEGIN
    CREATE INDEX idx_course_flag_course_id ON course_flag(course_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_course_flag_user_id' AND object_id = OBJECT_ID('course_flag'))
BEGIN
    CREATE INDEX idx_course_flag_user_id ON course_flag(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_course_flag_created_at' AND object_id = OBJECT_ID('course_flag'))
BEGIN
    CREATE INDEX idx_course_flag_created_at ON course_flag(created_at);
END
GO

-- ============================================
-- PART 2: QUIZ RELATED TABLES
-- ============================================

-- Table: questions
PRINT 'Creating table: questions';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'questions' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE questions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        quiz_id BIGINT NOT NULL,
        content NVARCHAR(MAX) NOT NULL,
        question_type VARCHAR(30) NOT NULL DEFAULT 'SINGLE_CHOICE',
        explanation NVARCHAR(MAX) NULL,
        order_index INT NOT NULL DEFAULT 1,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        deleted_flag BIT NULL DEFAULT 0,
        
        CONSTRAINT fk_questions_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
    );
    PRINT 'Created table: questions';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_questions_quiz_id' AND object_id = OBJECT_ID('questions'))
BEGIN
    CREATE INDEX idx_questions_quiz_id ON questions(quiz_id);
END
GO

-- Table: options
PRINT 'Creating table: options';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'options' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE options (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        question_id BIGINT NOT NULL,
        content NVARCHAR(MAX) NOT NULL,
        is_correct BIT NOT NULL DEFAULT 0,
        order_index INT NOT NULL DEFAULT 1,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions(id)
    );
    PRINT 'Created table: options';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_options_question_id' AND object_id = OBJECT_ID('options'))
BEGIN
    CREATE INDEX idx_options_question_id ON options(question_id);
END
GO

-- Table: quiz_attempts
PRINT 'Creating table: quiz_attempts';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'quiz_attempts' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE quiz_attempts (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        quiz_id BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
        started_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        submitted_at DATETIME2 NULL,
        score_percent INT NULL,
        correct_count INT NULL,
        total_questions INT NULL,
        
        CONSTRAINT fk_quiz_attempt_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_quiz_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
    );
    PRINT 'Created table: quiz_attempts';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_quiz_attempt_user' AND object_id = OBJECT_ID('quiz_attempts'))
BEGIN
    CREATE INDEX idx_quiz_attempt_user ON quiz_attempts(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_quiz_attempt_quiz' AND object_id = OBJECT_ID('quiz_attempts'))
BEGIN
    CREATE INDEX idx_quiz_attempt_quiz ON quiz_attempts(quiz_id);
END
GO

-- Table: quiz_answers
PRINT 'Creating table: quiz_answers';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'quiz_answers' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE quiz_answers (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        attempt_id BIGINT NOT NULL,
        question_id BIGINT NOT NULL,
        option_id BIGINT NULL,
        is_correct BIT NULL,
        
        CONSTRAINT fk_quiz_answer_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id),
        CONSTRAINT fk_quiz_answer_question FOREIGN KEY (question_id) REFERENCES questions(id),
        CONSTRAINT fk_quiz_answer_option FOREIGN KEY (option_id) REFERENCES options(id),
        CONSTRAINT uk_attempt_question UNIQUE (attempt_id, question_id)
    );
    PRINT 'Created table: quiz_answers';
END
GO

-- ============================================
-- PART 3: FLASHCARD TABLES
-- ============================================

-- Table: flashcard_sets
PRINT 'Creating table: flashcard_sets';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'flashcard_sets' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE flashcard_sets (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        created_by_user_id BIGINT NOT NULL,
        title NVARCHAR(255) NOT NULL,
        description NVARCHAR(1000) NULL,
        level VARCHAR(50) NULL,
        type VARCHAR(50) NOT NULL,
        section_content_id BIGINT NULL,
        deleted_flag BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_flashcard_set_user FOREIGN KEY (created_by_user_id) REFERENCES users(id),
        CONSTRAINT fk_flashcard_set_section_content FOREIGN KEY (section_content_id) REFERENCES sections_content(id)
    );
    PRINT 'Created table: flashcard_sets';
END
GO

-- Table: flashcards
PRINT 'Creating table: flashcards';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'flashcards' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE flashcards (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        set_id BIGINT NOT NULL,
        front_text NVARCHAR(255) NOT NULL,
        back_text NVARCHAR(255) NOT NULL,
        reading NVARCHAR(255) NULL,
        example_sentence NVARCHAR(1000) NULL,
        order_index INT NULL,
        deleted_flag BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_flashcard_set FOREIGN KEY (set_id) REFERENCES flashcard_sets(id)
    );
    PRINT 'Created table: flashcards';
END
GO

-- Table: user_flashcard_progress
PRINT 'Creating table: user_flashcard_progress';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'user_flashcard_progress' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE user_flashcard_progress (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        flashcard_id BIGINT NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'NEW',
        mastered_at DATETIME2 NULL,
        last_reviewed_at DATETIME2 NULL,
        review_count INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_ufp_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_ufp_flashcard FOREIGN KEY (flashcard_id) REFERENCES flashcards(id),
        CONSTRAINT uk_user_flashcard_progress_user_card UNIQUE (user_id, flashcard_id)
    );
    PRINT 'Created table: user_flashcard_progress';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_ufp_user' AND object_id = OBJECT_ID('user_flashcard_progress'))
BEGIN
    CREATE INDEX ix_ufp_user ON user_flashcard_progress(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_ufp_last_reviewed' AND object_id = OBJECT_ID('user_flashcard_progress'))
BEGIN
    CREATE INDEX ix_ufp_last_reviewed ON user_flashcard_progress(last_reviewed_at);
END
GO

-- ============================================
-- PART 4: PROFILE APPROVAL TABLES
-- ============================================

-- Table: user_certificates
PRINT 'Creating table: user_certificates';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'user_certificates' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE user_certificates (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        title NVARCHAR(255) NOT NULL,
        issue_date DATE NULL,
        expiry_date DATE NULL,
        credential_id VARCHAR(255) NULL,
        credential_url VARCHAR(1000) NULL,
        file_url VARCHAR(1000) NULL,
        file_name VARCHAR(255) NULL,
        mime_type VARCHAR(100) NULL,
        file_size_bytes BIGINT NULL,
        storage_provider VARCHAR(50) NULL,
        verified_by BIGINT NULL,
        verified_at DATETIME2 NULL,
        note NVARCHAR(MAX) NULL,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_user_cert_user FOREIGN KEY (user_id) REFERENCES users(id)
    );
    PRINT 'Created table: user_certificates';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_user_cert_user' AND object_id = OBJECT_ID('user_certificates'))
BEGIN
    CREATE INDEX ix_user_cert_user ON user_certificates(user_id);
END
GO

-- Table: profile_approve_request
PRINT 'Creating table: profile_approve_request';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'profile_approve_request' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE profile_approve_request (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        submitted_at DATETIME2 NULL,
        reviewed_by BIGINT NULL,
        reviewed_at DATETIME2 NULL,
        note NVARCHAR(MAX) NULL,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_par_user FOREIGN KEY (user_id) REFERENCES users(id)
    );
    PRINT 'Created table: profile_approve_request';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_par_user' AND object_id = OBJECT_ID('profile_approve_request'))
BEGIN
    CREATE INDEX ix_par_user ON profile_approve_request(user_id);
END
GO

-- Add foreign key from users to profile_approve_request if not exists
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_users_profile_approval_request' AND parent_object_id = OBJECT_ID('users'))
BEGIN
    ALTER TABLE users ADD CONSTRAINT fk_users_profile_approval_request FOREIGN KEY (profile_approval_request_id) REFERENCES profile_approve_request(id);
END
GO

-- Table: profile_approve_request_item
PRINT 'Creating table: profile_approve_request_item';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'profile_approve_request_item' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE profile_approve_request_item (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        request_id BIGINT NOT NULL,
        source_certificate_id BIGINT NULL,
        title NVARCHAR(255) NOT NULL,
        issue_date DATE NULL,
        expiry_date DATE NULL,
        credential_id VARCHAR(255) NULL,
        credential_url VARCHAR(1000) NULL,
        file_url VARCHAR(1000) NULL,
        file_name VARCHAR(255) NULL,
        mime_type VARCHAR(100) NULL,
        file_size_bytes BIGINT NULL,
        storage_provider VARCHAR(50) NULL,
        verified_by BIGINT NULL,
        verified_at DATETIME2 NULL,
        note NVARCHAR(MAX) NULL,
        created_at DATETIME2 NULL,
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_pari_request FOREIGN KEY (request_id) REFERENCES profile_approve_request(id),
        CONSTRAINT fk_pari_source_cert FOREIGN KEY (source_certificate_id) REFERENCES user_certificates(id)
    );
    PRINT 'Created table: profile_approve_request_item';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_pari_request' AND object_id = OBJECT_ID('profile_approve_request_item'))
BEGIN
    CREATE INDEX ix_pari_request ON profile_approve_request_item(request_id);
END
GO

-- ============================================
-- PART 5: FILE STORAGE & UTILITY TABLES
-- ============================================

-- Table: file_storage
PRINT 'Creating table: file_storage';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'file_storage' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE file_storage (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        file_path VARCHAR(500) NOT NULL UNIQUE,
        file_name VARCHAR(255) NULL,
        content_type VARCHAR(100) NULL,
        file_data VARBINARY(MAX) NOT NULL, -- SQL Server equivalent of BYTEA
        file_size_bytes BIGINT NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0
    );
    PRINT 'Created table: file_storage';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_file_storage_path' AND object_id = OBJECT_ID('file_storage'))
BEGIN
    CREATE INDEX idx_file_storage_path ON file_storage(file_path);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_file_storage_deleted' AND object_id = OBJECT_ID('file_storage'))
BEGIN
    CREATE INDEX idx_file_storage_deleted ON file_storage(deleted_flag);
END
GO

-- Table: password_reset_otp
PRINT 'Creating table: password_reset_otp';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'password_reset_otp' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE password_reset_otp (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        email VARCHAR(255) NOT NULL,
        otp_code VARCHAR(6) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        is_used BIT NOT NULL DEFAULT 0,
        failed_attempts INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE()
    );
    PRINT 'Created table: password_reset_otp';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_otp_email' AND object_id = OBJECT_ID('password_reset_otp'))
BEGIN
    CREATE INDEX idx_otp_email ON password_reset_otp(email);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_otp_code' AND object_id = OBJECT_ID('password_reset_otp'))
BEGIN
    CREATE INDEX idx_otp_code ON password_reset_otp(otp_code);
END
GO

-- Table: notifications
PRINT 'Creating table: notifications';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'notifications' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE notifications (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        type VARCHAR(50) NOT NULL,
        title NVARCHAR(500) NOT NULL,
        message NVARCHAR(MAX) NULL,
        is_read BIT NOT NULL DEFAULT 0,
        read_at DATETIME2 NULL,
        related_course_id BIGINT NULL,
        related_payment_id BIGINT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0
    );
    PRINT 'Created table: notifications';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_notification_user_id' AND object_id = OBJECT_ID('notifications'))
BEGIN
    CREATE INDEX idx_notification_user_id ON notifications(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_notification_read' AND object_id = OBJECT_ID('notifications'))
BEGIN
    CREATE INDEX idx_notification_read ON notifications(is_read);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_notification_created_at' AND object_id = OBJECT_ID('notifications'))
BEGIN
    CREATE INDEX idx_notification_created_at ON notifications(created_at);
END
GO

-- ============================================
-- PART 6: AI PACKAGE TABLES
-- ============================================

-- Table: ai_packages
PRINT 'Creating table: ai_packages';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ai_packages' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE ai_packages (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description NVARCHAR(500) NULL,
        price_cents BIGINT NOT NULL,
        currency VARCHAR(10) NULL DEFAULT 'VND',
        duration_days INT NOT NULL,
        grammar_quota INT NULL,
        kaiwa_quota INT NULL,
        pronun_quota INT NULL,
        conversation_quota INT NULL,
        is_active BIT NOT NULL DEFAULT 1,
        display_order INT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0
    );
    PRINT 'Created table: ai_packages';
END
GO

-- Table: ai_package_purchases
PRINT 'Creating table: ai_package_purchases';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ai_package_purchases' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE ai_package_purchases (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        package_id BIGINT NOT NULL,
        purchase_price_cents BIGINT NOT NULL,
        payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        purchased_at DATETIME2 NULL,
        expires_at DATETIME2 NULL,
        is_active BIT NOT NULL DEFAULT 0,
        transaction_id VARCHAR(255) NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_ai_purchase_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_ai_purchase_package FOREIGN KEY (package_id) REFERENCES ai_packages(id)
    );
    PRINT 'Created table: ai_package_purchases';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_ai_purchase_user' AND object_id = OBJECT_ID('ai_package_purchases'))
BEGIN
    CREATE INDEX idx_ai_purchase_user ON ai_package_purchases(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_ai_purchase_status' AND object_id = OBJECT_ID('ai_package_purchases'))
BEGIN
    CREATE INDEX idx_ai_purchase_status ON ai_package_purchases(payment_status);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_ai_purchase_active' AND object_id = OBJECT_ID('ai_package_purchases'))
BEGIN
    CREATE INDEX idx_ai_purchase_active ON ai_package_purchases(user_id, is_active);
END
GO

-- Table: ai_quotas (update with correct structure)
PRINT 'Updating table: ai_quotas';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ai_quotas' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE ai_quotas (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        service_type VARCHAR(20) NOT NULL,
        remaining_quota INT NULL,
        total_quota INT NULL,
        last_reset_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_ai_quota_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT uk_user_service UNIQUE (user_id, service_type),
        CONSTRAINT ai_quotas_service_type_check CHECK (service_type IN ('GRAMMAR', 'KAIWA', 'PRONUN', 'CONVERSATION'))
    );
    PRINT 'Created table: ai_quotas';
END
ELSE
BEGIN
    -- Update constraint if table exists
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ai_quotas_service_type_check' AND parent_object_id = OBJECT_ID('ai_quotas'))
    BEGIN
        ALTER TABLE ai_quotas DROP CONSTRAINT ai_quotas_service_type_check;
    END
    ALTER TABLE ai_quotas 
    ADD CONSTRAINT ai_quotas_service_type_check 
    CHECK (service_type IN ('GRAMMAR', 'KAIWA', 'PRONUN', 'CONVERSATION'));
    PRINT 'Updated ai_quotas constraint';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_ai_quota_user' AND object_id = OBJECT_ID('ai_quotas'))
BEGIN
    CREATE INDEX idx_ai_quota_user ON ai_quotas(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_ai_quota_service' AND object_id = OBJECT_ID('ai_quotas'))
BEGIN
    CREATE INDEX idx_ai_quota_service ON ai_quotas(service_type);
END
GO

-- Table: ai_translation_history
PRINT 'Creating table: ai_translation_history';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ai_translation_history' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE ai_translation_history (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NULL,
        source_language VARCHAR(50) NULL,
        target_language VARCHAR(50) NULL,
        original_text NVARCHAR(MAX) NULL,
        translated_text NVARCHAR(MAX) NULL,
        created_at DATETIME2 NULL DEFAULT GETDATE()
    );
    PRINT 'Created table: ai_translation_history';
END
GO

-- Table: ai_sentiment_history
PRINT 'Creating table: ai_sentiment_history';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'ai_sentiment_history' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE ai_sentiment_history (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NULL,
        text NVARCHAR(MAX) NULL,
        sentiment_score VARCHAR(50) NULL,
        magnitude VARCHAR(50) NULL,
        details NVARCHAR(MAX) NULL,
        created_at DATETIME2 NULL DEFAULT GETDATE()
    );
    PRINT 'Created table: ai_sentiment_history';
END
GO

-- ============================================
-- PART 7: COURSE FEEDBACK & CERTIFICATES
-- ============================================

-- Table: course_feedback
PRINT 'Creating table: course_feedback';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'course_feedback' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE course_feedback (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        course_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        rating INT NOT NULL,
        comment NVARCHAR(2000) NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_course_reviews_course FOREIGN KEY (course_id) REFERENCES course(id),
        CONSTRAINT fk_course_reviews_user FOREIGN KEY (user_id) REFERENCES users(id)
    );
    PRINT 'Created table: course_feedback';
END
GO

-- Table: course_completion_certificates
PRINT 'Creating table: course_completion_certificates';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'course_completion_certificates' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE course_completion_certificates (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        enrollment_id BIGINT NOT NULL UNIQUE,
        user_id BIGINT NOT NULL,
        course_id BIGINT NOT NULL,
        course_title NVARCHAR(500) NULL,
        completed_at DATETIME2 NOT NULL,
        issued_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        certificate_number VARCHAR(100) UNIQUE NULL,
        
        CONSTRAINT uk_cert_enrollment UNIQUE (enrollment_id)
    );
    PRINT 'Created table: course_completion_certificates';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_cert_enrollment_id' AND object_id = OBJECT_ID('course_completion_certificates'))
BEGIN
    CREATE INDEX idx_cert_enrollment_id ON course_completion_certificates(enrollment_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_cert_user_id' AND object_id = OBJECT_ID('course_completion_certificates'))
BEGIN
    CREATE INDEX idx_cert_user_id ON course_completion_certificates(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_cert_course_id' AND object_id = OBJECT_ID('course_completion_certificates'))
BEGIN
    CREATE INDEX idx_cert_course_id ON course_completion_certificates(course_id);
END
GO

-- Table: user_daily_learning
PRINT 'Creating table: user_daily_learning';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'user_daily_learning' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE user_daily_learning (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        learning_date DATE NOT NULL,
        activity_count INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT uk_user_daily_learning_user_date UNIQUE (user_id, learning_date)
    );
    PRINT 'Created table: user_daily_learning';
END
GO

-- ============================================
-- PART 8: JLPT TEST TABLES
-- ============================================

-- Table: jlpt_events
PRINT 'Creating table: jlpt_events';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'jlpt_events' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE jlpt_events (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        created_by BIGINT NOT NULL,
        title NVARCHAR(255) NOT NULL,
        level VARCHAR(10) NOT NULL,
        description NVARCHAR(1000) NULL,
        status VARCHAR(20) NOT NULL,
        start_at DATETIME2 NOT NULL,
        end_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_jlpt_event_user FOREIGN KEY (created_by) REFERENCES users(id)
    );
    PRINT 'Created table: jlpt_events';
END
GO

-- Table: jlpt_tests
PRINT 'Creating table: jlpt_tests';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'jlpt_tests' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE jlpt_tests (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        event_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        level VARCHAR(10) NOT NULL,
        duration_min INT NOT NULL,
        total_score INT NOT NULL,
        result NVARCHAR(500) NULL,
        current_participants INT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_jlpt_test_event FOREIGN KEY (event_id) REFERENCES jlpt_events(id),
        CONSTRAINT fk_jlpt_test_user FOREIGN KEY (user_id) REFERENCES users(id)
    );
    PRINT 'Created table: jlpt_tests';
END
GO

-- Table: jlpt_questions
PRINT 'Creating table: jlpt_questions';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'jlpt_questions' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE jlpt_questions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        test_id BIGINT NOT NULL,
        content NVARCHAR(1000) NOT NULL,
        question_type VARCHAR(30) NOT NULL,
        explanation NVARCHAR(2000) NULL,
        order_index INT NULL,
        audio_path VARCHAR(500) NULL,
        image_path VARCHAR(500) NULL,
        audio_url VARCHAR(500) NULL,
        transcript NVARCHAR(MAX) NULL,
        image_alt_text NVARCHAR(255) NULL,
        updated_at DATETIME2 NULL,
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_jlpt_question_test FOREIGN KEY (test_id) REFERENCES jlpt_tests(id)
    );
    PRINT 'Created table: jlpt_questions';
END
GO

-- Table: jlpt_options
PRINT 'Creating table: jlpt_options';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'jlpt_options' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE jlpt_options (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        question_id BIGINT NOT NULL,
        content NVARCHAR(1000) NOT NULL,
        is_correct BIT NOT NULL DEFAULT 0,
        order_index INT NULL,
        image_path VARCHAR(500) NULL,
        image_alt_text NVARCHAR(255) NULL,
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_jlpt_option_question FOREIGN KEY (question_id) REFERENCES jlpt_questions(id)
    );
    PRINT 'Created table: jlpt_options';
END
GO

-- Table: jlpt_test_attempts
PRINT 'Creating table: jlpt_test_attempts';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'jlpt_test_attempts' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE jlpt_test_attempts (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        test_id BIGINT NOT NULL,
        started_at DATETIME2 NOT NULL,
        submitted_at DATETIME2 NOT NULL,
        total_questions INT NOT NULL,
        correct_count INT NOT NULL,
        score FLOAT NOT NULL,
        passed BIT NOT NULL,
        grammar_vocab_total INT NULL,
        grammar_vocab_correct INT NULL,
        grammar_vocab_score FLOAT NULL,
        reading_total INT NULL,
        reading_correct INT NULL,
        reading_score FLOAT NULL,
        listening_total INT NULL,
        listening_correct INT NULL,
        listening_score FLOAT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_attempt_test FOREIGN KEY (test_id) REFERENCES jlpt_tests(id)
    );
    PRINT 'Created table: jlpt_test_attempts';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_jlpt_attempt_user' AND object_id = OBJECT_ID('jlpt_test_attempts'))
BEGIN
    CREATE INDEX idx_jlpt_attempt_user ON jlpt_test_attempts(user_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_jlpt_attempt_test' AND object_id = OBJECT_ID('jlpt_test_attempts'))
BEGIN
    CREATE INDEX idx_jlpt_attempt_test ON jlpt_test_attempts(test_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_jlpt_attempt_submitted' AND object_id = OBJECT_ID('jlpt_test_attempts'))
BEGIN
    CREATE INDEX idx_jlpt_attempt_submitted ON jlpt_test_attempts(submitted_at);
END
GO

-- Table: jlpt_test_attempt_answers
PRINT 'Creating table: jlpt_test_attempt_answers';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'jlpt_test_attempt_answers' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE jlpt_test_attempt_answers (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        attempt_id BIGINT NOT NULL,
        question_id BIGINT NOT NULL,
        selected_option_id BIGINT NULL,
        correct_option_id BIGINT NOT NULL,
        is_correct BIT NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT fk_jlpt_attempt_answer_attempt FOREIGN KEY (attempt_id) REFERENCES jlpt_test_attempts(id),
        CONSTRAINT fk_jlpt_attempt_answer_question FOREIGN KEY (question_id) REFERENCES jlpt_questions(id),
        CONSTRAINT fk_jlpt_attempt_answer_selected_option FOREIGN KEY (selected_option_id) REFERENCES jlpt_options(id),
        CONSTRAINT fk_jlpt_attempt_answer_correct_option FOREIGN KEY (correct_option_id) REFERENCES jlpt_options(id),
        CONSTRAINT uk_jlpt_attempt_answer_attempt_question UNIQUE (attempt_id, question_id)
    );
    PRINT 'Created table: jlpt_test_attempt_answers';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_jlpt_attempt_answer_attempt' AND object_id = OBJECT_ID('jlpt_test_attempt_answers'))
BEGIN
    CREATE INDEX idx_jlpt_attempt_answer_attempt ON jlpt_test_attempt_answers(attempt_id);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_jlpt_attempt_answer_question' AND object_id = OBJECT_ID('jlpt_test_attempt_answers'))
BEGIN
    CREATE INDEX idx_jlpt_attempt_answer_question ON jlpt_test_attempt_answers(question_id);
END
GO

-- Table: jlpt_user_test_session
PRINT 'Creating table: jlpt_user_test_session';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'jlpt_user_test_session' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE jlpt_user_test_session (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        test_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        started_at DATETIME2 NOT NULL,
        expires_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        deleted_flag BIT NOT NULL DEFAULT 0,
        
        CONSTRAINT fk_jlpt_session_test FOREIGN KEY (test_id) REFERENCES jlpt_tests(id),
        CONSTRAINT fk_jlpt_session_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT uk_session_test_user UNIQUE (test_id, user_id)
    );
    PRINT 'Created table: jlpt_user_test_session';
END
GO

-- Table: jlpt_answers (legacy table, may not be used)
PRINT 'Creating table: jlpt_answers';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'jlpt_answers' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE jlpt_answers (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        test_id BIGINT NOT NULL,
        question_id BIGINT NOT NULL,
        selected_option_id BIGINT NOT NULL,
        is_correct BIT NOT NULL DEFAULT 0,
        answered_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT fk_jlpt_answer_user FOREIGN KEY (user_id) REFERENCES users(id),
        CONSTRAINT fk_jlpt_answer_test FOREIGN KEY (test_id) REFERENCES jlpt_tests(id),
        CONSTRAINT fk_jlpt_answer_question FOREIGN KEY (question_id) REFERENCES jlpt_questions(id),
        CONSTRAINT fk_jlpt_answer_option FOREIGN KEY (selected_option_id) REFERENCES jlpt_options(id),
        CONSTRAINT uk_jlpt_answer_user_test_question UNIQUE (user_id, test_id, question_id)
    );
    PRINT 'Created table: jlpt_answers';
END
GO

-- ============================================
-- PART 9: POLICY TABLE
-- ============================================

-- Table: policies
PRINT 'Creating table: policies';
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'policies' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
    CREATE TABLE policies (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        role_id BIGINT NOT NULL,
        title NVARCHAR(255) NOT NULL,
        content NVARCHAR(MAX) NULL,
        created_by BIGINT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        
        CONSTRAINT fk_policies_role FOREIGN KEY (role_id) REFERENCES roles(id),
        CONSTRAINT fk_policies_created_by FOREIGN KEY (created_by) REFERENCES users(id)
    );
    PRINT 'Created table: policies';
END
GO

-- ============================================
-- PART 10: SUMMARY
-- ============================================
PRINT '';
PRINT '============================================';
PRINT 'DATABASE SCHEMA CREATION COMPLETED';
PRINT '============================================';
PRINT '';
PRINT 'Total tables created: 48';
PRINT '';
PRINT 'Core Tables:';
PRINT '  1. roles';
PRINT '  2. users (with bank account fields)';
PRINT '  3. course (with pending_update_at and snapshot_data)';
PRINT '  4. chapter';
PRINT '  5. lessons';
PRINT '  6. sections (with QUIZ study_type)';
PRINT '  7. sections_content (with QUIZ content_format)';
PRINT '  8. quizzes (with section_id)';
PRINT '  9. enrollment';
PRINT '  10. user_content_progress';
PRINT '  11. cart';
PRINT '  12. cartitem';
PRINT '  13. payment';
PRINT '  14. teacher_revenue';
PRINT '  15. wallet_transactions';
PRINT '  16. course_comment';
PRINT '  17. course_flag';
PRINT '';
PRINT 'Quiz Related Tables:';
PRINT '  18. questions';
PRINT '  19. options';
PRINT '  20. quiz_attempts';
PRINT '  21. quiz_answers';
PRINT '';
PRINT 'Flashcard Tables:';
PRINT '  22. flashcard_sets';
PRINT '  23. flashcards';
PRINT '  24. user_flashcard_progress';
PRINT '';
PRINT 'Profile Approval Tables:';
PRINT '  25. user_certificates';
PRINT '  26. profile_approve_request';
PRINT '  27. profile_approve_request_item';
PRINT '';
PRINT 'File Storage & Utility Tables:';
PRINT '  28. file_storage';
PRINT '  29. password_reset_otp';
PRINT '  30. notifications';
PRINT '';
PRINT 'AI Package Tables:';
PRINT '  31. ai_packages';
PRINT '  32. ai_package_purchases';
PRINT '  33. ai_quotas (with CONVERSATION service_type)';
PRINT '  34. ai_translation_history';
PRINT '  35. ai_sentiment_history';
PRINT '';
PRINT 'Course Feedback & Certificates:';
PRINT '  36. course_feedback';
PRINT '  37. course_completion_certificates';
PRINT '  38. user_daily_learning';
PRINT '';
PRINT 'JLPT Test Tables:';
PRINT '  39. jlpt_events';
PRINT '  40. jlpt_tests';
PRINT '  41. jlpt_questions';
PRINT '  42. jlpt_options';
PRINT '  43. jlpt_test_attempts';
PRINT '  44. jlpt_test_attempt_answers';
PRINT '  45. jlpt_user_test_session';
PRINT '  46. jlpt_answers';
PRINT '';
PRINT 'Policy Table:';
PRINT '  47. policies';
PRINT '';
PRINT 'All constraints, foreign keys, unique constraints, check constraints, and indexes applied!';
PRINT 'All migrations included!';
PRINT 'Script execution completed successfully!';
GO

