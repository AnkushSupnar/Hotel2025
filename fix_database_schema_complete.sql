-- Complete Database Schema Fix for Employee ID Type Change
-- This script fixes the foreign key constraint issue between users and employee tables
-- Run this in your MySQL hotel2025 database

USE hotel2025;

-- Display current table structures
SELECT 'Current employee.Id column type:' AS 'Step 1';
SHOW COLUMNS FROM employee WHERE Field = 'Id';

SELECT 'Current users.employee_id column type:' AS 'Step 2';
SHOW COLUMNS FROM users WHERE Field = 'employee_id';

-- Step 3: Drop the foreign key constraint if it exists
SELECT 'Dropping foreign key constraint...' AS 'Step 3';
SET @dbname = DATABASE();
SET @tablename = 'users';
SET @constraintname = 'FKfndbe67uw6silwqnlyudtwqmo';

-- Drop foreign key if exists
SET @query = CONCAT('ALTER TABLE ', @tablename, ' DROP FOREIGN KEY ', @constraintname);
SET @check_fk = (SELECT COUNT(*)
                 FROM information_schema.TABLE_CONSTRAINTS
                 WHERE TABLE_SCHEMA = @dbname
                 AND TABLE_NAME = @tablename
                 AND CONSTRAINT_NAME = @constraintname);

SELECT IF(@check_fk > 0,
    CONCAT('Foreign key ', @constraintname, ' exists - will be dropped'),
    'Foreign key does not exist - skipping') AS 'Status';

-- Execute drop if exists
SET @drop_fk_sql = IF(@check_fk > 0,
    CONCAT('ALTER TABLE ', @tablename, ' DROP FOREIGN KEY ', @constraintname),
    'SELECT "Foreign key does not exist" AS Info');
PREPARE stmt FROM @drop_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 4: Check for orphaned records (users with invalid employee_id)
SELECT 'Checking for orphaned records...' AS 'Step 4';
SELECT COUNT(*) AS 'Orphaned Users Count'
FROM users u
WHERE u.employee_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM employee e WHERE e.Id = u.employee_id);

-- Step 5: Handle orphaned records - Set employee_id to NULL for orphaned users
SELECT 'Setting orphaned employee_id to NULL...' AS 'Step 5';
UPDATE users u
SET u.employee_id = NULL
WHERE u.employee_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM employee e WHERE e.Id = u.employee_id);

SELECT ROW_COUNT() AS 'Records Updated';

-- Step 6: Modify employee.Id column to BIGINT
SELECT 'Modifying employee.Id column to BIGINT...' AS 'Step 6';
ALTER TABLE employee MODIFY COLUMN Id BIGINT NOT NULL AUTO_INCREMENT;

-- Step 7: Modify users.employee_id column to BIGINT
SELECT 'Modifying users.employee_id column to BIGINT...' AS 'Step 7';
ALTER TABLE users MODIFY COLUMN employee_id BIGINT;

-- Step 8: Recreate the foreign key constraint
SELECT 'Recreating foreign key constraint...' AS 'Step 8';
ALTER TABLE users
ADD CONSTRAINT FKfndbe67uw6silwqnlyudtwqmo
FOREIGN KEY (employee_id) REFERENCES employee (Id);

-- Step 9: Verify the changes
SELECT 'Verification - employee.Id column:' AS 'Step 9';
SHOW COLUMNS FROM employee WHERE Field = 'Id';

SELECT 'Verification - users.employee_id column:' AS 'Step 10';
SHOW COLUMNS FROM users WHERE Field = 'employee_id';

SELECT 'Verification - Foreign Key Constraints:' AS 'Step 11';
SELECT
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'hotel2025'
AND TABLE_NAME = 'users'
AND CONSTRAINT_NAME = 'FKfndbe67uw6silwqnlyudtwqmo';

SELECT 'Schema fix completed successfully!' AS 'Result';
