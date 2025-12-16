-- ============================================
-- Migration: Add file_url column to file_storage table
-- ============================================
-- This migration adds file_url column to store Cloudflare R2 URLs
-- and makes file_data nullable (for backward compatibility during migration)
-- ============================================

-- Add file_url column if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'file_storage' AND column_name = 'file_url'
    ) THEN
        ALTER TABLE file_storage 
        ADD COLUMN file_url VARCHAR(1000) NULL;
        RAISE NOTICE 'Added column: file_url';
    END IF;
END $$;

-- Make file_data nullable (for backward compatibility)
-- Old files will still have file_data, new files will use file_url
DO $$
BEGIN
    -- Check if column exists and is NOT NULL
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'file_storage' 
        AND column_name = 'file_data' 
        AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE file_storage 
        ALTER COLUMN file_data DROP NOT NULL;
        RAISE NOTICE 'Made file_data nullable';
    END IF;
END $$;

-- Add index on file_url for faster lookups
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'file_storage' AND indexname = 'idx_file_storage_url'
    ) THEN
        CREATE INDEX idx_file_storage_url ON file_storage(file_url);
        RAISE NOTICE 'Added index: idx_file_storage_url';
    END IF;
END $$;

