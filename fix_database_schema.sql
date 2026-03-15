-- Fix database schema for Employee ID type change from INT to BIGINT
-- Run this SQL script in your hotel2025 database

USE hotel2025;

-- Step 1: Drop the foreign key constraint from users table
ALTER TABLE users DROP FOREIGN KEY IF EXISTS FKfndbe67uw6silwqnlyudtwqmo;

-- Step 2: Modify employee.Id column from INT to BIGINT
ALTER TABLE employee MODIFY COLUMN Id BIGINT NOT NULL AUTO_INCREMENT;

-- Step 3: Modify users.employee_id column to BIGINT (if it exists)
ALTER TABLE users MODIFY COLUMN employee_id BIGINT;

-- Step 4: Re-create the foreign key constraint with correct types
ALTER TABLE users
ADD CONSTRAINT FKfndbe67uw6silwqnlyudtwqmo
FOREIGN KEY (employee_id) REFERENCES employee (Id);

-- Verify the changes
SHOW COLUMNS FROM employee WHERE Field = 'Id';
SHOW COLUMNS FROM users WHERE Field = 'employee_id';
