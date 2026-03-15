# Database Setup Guide

## Overview
This application has been converted from API-based to a standalone MySQL database application.

## Prerequisites
- MySQL Server 8.0+ running on localhost:3306
- Database credentials configured in `application.properties`

## Setup Steps

### 1. Ensure MySQL is Running
Make sure MySQL is running on `localhost:3306`

### 2. Database Configuration
The application is configured to use:
- **Database**: `hotel2025`
- **Username**: `root`
- **Password**: `2355`

You can modify these in `src/main/resources/application.properties`

### 3. First Run
On first run, the application will automatically:
- Create the database `hotel2025` (if it doesn't exist)
- Create all required tables using Hibernate DDL

### 4. Create Initial Users
After the first run, execute the SQL script to create initial users:

```bash
mysql -u root -p2355 < setup_initial_user.sql
```

Or manually run the SQL commands in MySQL:

```sql
USE hotel2025;

-- Admin user (username: admin, password: admin)
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36qK9B3nS4bw7K7f8Yjqkla', 'ADMIN', NOW(), NOW());

-- Regular user (username: user, password: user)
INSERT INTO users (username, password, role, created_at, updated_at)
VALUES ('user', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', NOW(), NOW());
```

## Login Credentials

After running the setup script, you can login with:

- **Admin Account**
  - Username: `admin`
  - Password: `admin`

- **User Account**
  - Username: `user`
  - Password: `user`

## Database Schema

The application creates the following tables:

### Core Tables
- `users` - User authentication and roles
- `category_master` - Product/menu categories
- `menu_items` - Menu items with pricing
- `orders` - Customer orders
- `order_items` - Items within each order
- `billings` - Billing and payment information

### Relationships
- `menu_items.category_id` → `category_master.id`
- `order_items.order_id` → `orders.id`
- `order_items.menu_item_id` → `menu_items.id`
- `orders.created_by` → `users.id`
- `billings.order_id` → `orders.id`
- `billings.created_by` → `users.id`

## Troubleshooting

### Connection Issues
If you get database connection errors:
1. Verify MySQL is running: `mysql -u root -p`
2. Check credentials in `application.properties`
3. Ensure the database exists: `CREATE DATABASE hotel2025;`

### Table Creation Issues
If tables aren't created automatically:
1. Check logs for Hibernate errors
2. Verify `spring.jpa.hibernate.ddl-auto=update` in properties
3. Ensure user has CREATE privileges

### Login Issues
If you can't login:
1. Verify users exist: `SELECT * FROM users;`
2. Check password encoding is correct
3. Run the `setup_initial_user.sql` script again

## Running the Application

```bash
mvn clean install
mvn spring-boot:run
```

Or run the `Main.java` class directly from your IDE.
