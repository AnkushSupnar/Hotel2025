# Hibernate DDL Auto Configuration Guide

## What Is Happening?

When you see SQL statements like:
```sql
alter table billings modify column payment_method enum (...)
alter table orders modify column status enum (...)
```

This is **Hibernate automatically synchronizing your database schema** with your Java entity classes.

## Why Does This Happen?

Your configuration in `application.properties`:
```properties
spring.jpa.hibernate.ddl-auto=update
```

This tells Hibernate to:
1. ✅ Scan all `@Entity` classes when app starts
2. ✅ Compare entity definitions with database tables
3. ✅ Generate ALTER TABLE statements to sync them
4. ✅ Execute the SQL to update schema

## Is This Normal?

**Yes, absolutely normal!** ✅

This happens when:
- First time running the app (creates tables)
- Entity classes are modified (adds/updates columns)
- Enum values change (updates enum definitions)
- Database schema differs from entities

## DDL Auto Options

### 1. `update` (Current - Development)
```properties
spring.jpa.hibernate.ddl-auto=update
```

**What it does:**
- Creates tables if they don't exist
- Adds new columns
- Modifies existing columns
- **Never drops tables or columns**
- **Never deletes data**

**Use for:** Development, Testing

**Safe?** ✅ Yes - doesn't delete anything

---

### 2. `validate`
```properties
spring.jpa.hibernate.ddl-auto=validate
```

**What it does:**
- Validates schema matches entities
- **Throws error if mismatch**
- Makes NO changes to database

**Use for:** Production (after manual migrations)

**Safe?** ✅ Yes - read-only check

---

### 3. `create`
```properties
spring.jpa.hibernate.ddl-auto=create
```

**What it does:**
- **DROPS all tables**
- Creates fresh schema
- **DELETES ALL DATA**

**Use for:** Clean testing, demos

**Safe?** ❌ NO - destroys all data!

---

### 4. `create-drop`
```properties
spring.jpa.hibernate.ddl-auto=create-drop
```

**What it does:**
- Drops tables on startup
- Creates fresh schema
- **Drops tables again on shutdown**

**Use for:** Integration tests

**Safe?** ❌ NO - destroys all data twice!

---

### 5. `none`
```properties
spring.jpa.hibernate.ddl-auto=none
```

**What it does:**
- Does absolutely nothing
- No validation, no changes

**Use for:** Production (with manual migrations)

**Safe?** ✅ Yes - does nothing

## Recommended Settings

### Development
```properties
# Let Hibernate manage schema
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=INFO
```

**Why:**
- Automatic schema updates
- Less verbose logging
- Safe - doesn't delete data

### Production
```properties
# No automatic changes
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=WARN
```

**Why:**
- Validates schema
- Fails fast if mismatch
- No automatic modifications
- Requires manual migrations (Flyway/Liquibase)

### Testing
```properties
# Fresh database each run
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
```

**Why:**
- Clean state for each test
- Automatic cleanup

## Current Project Settings

### Updated Configuration (Less Verbose)
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=INFO
```

**What changed:**
- ❌ SQL statements no longer printed to console
- ✅ Still logged to file (./logs)
- ✅ Schema still auto-updates
- ✅ Much cleaner console output

### To See SQL Again (if needed)
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

## Understanding the SQL Logs

### What You Saw:
```sql
alter table billings
  modify column payment_method enum ('CARD','CASH','ONLINE','OTHER','UPI')
```

**Why it happened:**
Your `PaymentMethod` enum in Java:
```java
public enum PaymentMethod {
    CASH, CARD, UPI, ONLINE, OTHER
}
```

Hibernate converts this to a MySQL ENUM type and ensures the database matches.

### Common SQL Operations:

#### Creating Tables (First Run)
```sql
create table users (
    id bigint not null auto_increment,
    username varchar(50) not null,
    ...
)
```

#### Adding Columns (Entity Modified)
```sql
alter table users add column email varchar(255)
```

#### Modifying Columns (Type Changed)
```sql
alter table orders modify column total_amount decimal(10,2)
```

#### Creating Indexes
```sql
create index idx_username on users (username)
```

## Best Practices

### ✅ DO:
1. Use `update` during development
2. Use `validate` in production
3. Keep SQL logging off in production
4. Review generated SQL in development
5. Use migration tools (Flyway/Liquibase) for production

### ❌ DON'T:
1. Use `create` or `create-drop` in production
2. Use `update` in production without testing
3. Ignore schema validation errors
4. Rely on auto-DDL for complex migrations
5. Enable verbose SQL logging in production

## Troubleshooting

### Schema validation failed
**Error:** Schema-validation: missing column [...]

**Solution:**
```properties
# Temporarily switch to update
spring.jpa.hibernate.ddl-auto=update
```

### Too many SQL logs
**Solution:**
```properties
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=INFO
```

### Want to see what Hibernate will do
**Solution:**
```properties
# Test mode - show SQL but don't execute
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
```

## Migration to Production

When ready for production:

### Step 1: Export Schema
```bash
mysqldump -u root -p --no-data hotel2025 > schema.sql
```

### Step 2: Switch to Validate
```properties
spring.jpa.hibernate.ddl-auto=validate
```

### Step 3: Use Migration Tool
Add Flyway or Liquibase for version control of schema changes.

### Step 4: Manual Changes Only
All schema changes go through migration scripts, not Hibernate.

## Summary

| Environment | Setting | SQL Logs | Purpose |
|-------------|---------|----------|---------|
| Development | `update` | INFO | Auto-updates, less noise |
| Staging | `validate` | WARN | Check schema before deploy |
| Production | `validate` | ERROR | Fail fast, manual migrations |
| Testing | `create-drop` | OFF | Clean state per test |

## Your Current Setup ✅

```properties
# Safe for development
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=INFO
```

**This means:**
- ✅ Schema auto-updates when you change entities
- ✅ No data loss
- ✅ Clean console (SQL logged to file only)
- ✅ Perfect for development

**You're all set!** The SQL execution is normal and expected. 🎉
