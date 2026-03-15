# Quick Start Guide - Hotel Management System

## ✅ Setup Complete!

Your hotel management system has been successfully configured with:
- ✅ MySQL database integration
- ✅ Modern Material Design login UI
- ✅ Spring Data JPA repositories
- ✅ BCrypt password encryption

## 🚀 How to Run

### Step 1: Start MySQL
Ensure MySQL is running on `localhost:3306`

### Step 2: Build the Project
```bash
mvn clean install
```

### Step 3: Run the Application
```bash
mvn spring-boot:run
```

Or run the `Main.java` class from your IDE.

### Step 4: Setup Initial Users (First Time Only)
After the first run, the database and tables will be created automatically. Then run:
```bash
mysql -u root -p2355 < setup_initial_user.sql
```

## 🔐 Login Credentials

After running the setup script:

- **Admin Account**
  - Username: `admin`
  - Password: `admin`

- **User Account**
  - Username: `user`
  - Password: `user`

## 🎨 New UI Features

The login screen now features:
- **Modern Material Design** with purple gradient background
- **Two-panel layout**: Brand section + Login form
- **Unicode icons**: 🏨 Hotel, 👤 User, 🔒 Lock, ✓ Checkmarks
- **Smooth animations** on hover and click
- **Professional card design** with shadows and borders
- **Responsive layout** that looks great on different screen sizes

## 📊 Database Configuration

Current settings in `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hotel2025
spring.datasource.username=root
spring.datasource.password=2355
```

Change these if your MySQL credentials are different.

## 🗄️ Database Schema

The application automatically creates these tables:
- `users` - User authentication
- `category_master` - Categories
- `menu_items` - Menu items with pricing
- `orders` - Customer orders
- `order_items` - Order line items
- `billings` - Billing and payments

## 🔧 Troubleshooting

### Can't connect to database?
1. Check MySQL is running
2. Verify credentials in `application.properties`
3. Ensure database `hotel2025` exists

### Login not working?
1. Run the `setup_initial_user.sql` script
2. Check the `users` table exists: `SELECT * FROM hotel2025.users;`

### UI looks broken?
1. Clear the build: `mvn clean`
2. Rebuild: `mvn clean install`
3. Run again

## 📚 Documentation Files

- `DATABASE_SETUP.md` - Complete database setup guide
- `UI_DESIGN.md` - UI design documentation
- `setup_initial_user.sql` - SQL script for initial data

## 🎯 What's Next?

After logging in, you can:
1. Manage categories
2. Create menu items
3. Process orders
4. Generate bills
5. View reports

## 💡 Tips

- The server URL field is now hidden (database mode)
- All data is stored locally in MySQL
- Sessions are managed in-memory
- Check logs in `./logs` directory for debugging

## 🆘 Need Help?

Check the error logs in the `logs/` folder or review the stack trace in the console for detailed error information.

---

**Enjoy your new Hotel Management System!** 🎉
