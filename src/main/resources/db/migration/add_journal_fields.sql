-- Journal Table Migration
-- Add title and mood columns to journals table

-- Check if columns exist before adding (MySQL)
ALTER TABLE journals 
ADD COLUMN IF NOT EXISTS title VARCHAR(255) AFTER user_id,
ADD COLUMN IF NOT EXISTS mood VARCHAR(20) AFTER content;

-- If using PostgreSQL, use this instead:
-- ALTER TABLE journals 
-- ADD COLUMN IF NOT EXISTS title VARCHAR(255),
-- ADD COLUMN IF NOT EXISTS mood VARCHAR(20);

-- Verify the changes
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'journals' 
ORDER BY ORDINAL_POSITION;
