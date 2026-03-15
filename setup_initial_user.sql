-- Initial Database Setup for Hotel Management System
-- Run this script after the application creates the tables

USE hotel2025;

-- Create test users for autocomplete testing
-- Note: Passwords are stored as plain text for development purposes

-- Admin user
-- Username: admin, Password: admin
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('admin', 'admin', 'ADMIN', NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- Regular users
-- Username: user, Password: user
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('user', 'user', 'USER', NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- Additional test users for autocomplete
-- Username: alice, Password: alice123
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('alice', 'alice123', 'USER', NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- Username: andrew, Password: andrew123
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('andrew', 'andrew123', 'USER', NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- Username: anna, Password: anna123
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('anna', 'anna123', 'USER', NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- Username: bob, Password: bob123
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('bob', 'bob123', 'USER', NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- Username: manager, Password: manager123
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('manager', 'manager123', 'MANAGER', NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- Create sample categories
INSERT INTO category_master (category, stock, created_at, updated_at)
VALUES
    ('Beverages', 'In Stock', NOW(), NOW()),
    ('Main Course', 'In Stock', NOW(), NOW()),
    ('Desserts', 'In Stock', NOW(), NOW()),
    ('Starters', 'In Stock', NOW(), NOW())
ON DUPLICATE KEY UPDATE category=category;

SELECT 'Initial setup completed!' as Status;
SELECT 'You can now login with:' as '';
SELECT 'Username: admin, Password: admin' as 'Admin Account';
SELECT 'Username: user, Password: user' as 'User Account';
SELECT 'Test autocomplete by typing: alice, andrew, anna, bob, manager' as 'Autocomplete Test Users';
