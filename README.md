# Hotel Management Desktop Application

A comprehensive JavaFX-based desktop application for hotel/restaurant management with direct database integration using Spring Boot and JPA.

## 📋 Overview

This is a **standalone desktop application** that directly connects to a MySQL database for managing hotel/restaurant operations. It features a modern Material Design UI, multi-language support (English and Marathi), and comprehensive business functionality.

## 🚀 Quick Start

```bash
# Navigate to project directory
cd hotel-frontend

# Run the application
mvn spring-boot:run
```

**Application Type**: Desktop Application (No web server required)

## 📦 Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **JavaFX Runtime 19+** (included in dependencies)

## 🗄️ Database Configuration

Configure database connection in `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/hotel_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Application Configuration
spring.main.web-application-type=none
```

## 📂 Project Structure

```
hotel-frontend/
│
├── src/main/java/com/frontend/
│   ├── Main.java                          # Application entry point
│   │
│   ├── controller/                        # UI Controllers
│   │   ├── LoginController.java           # Login screen
│   │   ├── HomeController.java            # Main dashboard
│   │   ├── MasterMenuController.java      # Master data menu
│   │   ├── CreateShopeController.java     # Restaurant registration
│   │   ├── master/                        # Master data controllers
│   │   │   ├── AddCategoryController.java
│   │   │   ├── AddItemController.java
│   │   │   ├── AddCustomerController.java
│   │   │   ├── AddEmployeeController.java
│   │   │   └── AddTableController.java
│   │   ├── transaction/                   # Transaction controllers
│   │   │   └── BillingController.java     # Billing & POS system
│   │   └── setting/                       # Settings controllers
│   │       ├── ApplicationSettingController.java
│   │       └── SettingMenuController.java
│   │
│   ├── service/                           # Business logic services
│   │   ├── SessionService.java            # User session management
│   │   ├── ShopService.java               # Restaurant management
│   │   ├── CustomerService.java           # Customer operations
│   │   ├── EmployeeService.java           # Employee management
│   │   ├── ItemService.java               # Menu item management
│   │   ├── TableMasterService.java        # Table management
│   │   ├── ApplicationSettingService.java # App settings
│   │   ├── AuthApiService.java            # Authentication
│   │   ├── CategoryApiService.java        # Category operations
│   │   └── RoleService.java               # User roles
│   │
│   ├── entity/                            # JPA Entities (Database models)
│   │   ├── User.java                      # User accounts
│   │   ├── Employee.java                  # Employee details
│   │   ├── Shop.java                      # Restaurant/shop info
│   │   ├── Customer.java                  # Customer details
│   │   ├── CategoryMaster.java            # Food categories
│   │   ├── Item.java                      # Menu items
│   │   ├── MenuItem.java                  # Menu configuration
│   │   ├── TableMaster.java               # Table configuration
│   │   ├── Order.java                     # Orders
│   │   ├── OrderItem.java                 # Order line items
│   │   ├── Billing.java                   # Billing records
│   │   ├── ApplicationSetting.java        # Application settings
│   │   ├── Role.java                      # User roles
│   │   ├── Designation.java               # Employee designations
│   │   ├── OrderStatus.java               # Order status enum
│   │   ├── PaymentMethod.java             # Payment methods enum
│   │   └── PaymentStatus.java             # Payment status enum
│   │
│   ├── repository/                        # JPA Repositories
│   │   └── (Auto-generated Spring Data JPA repositories)
│   │
│   ├── customUI/                          # Custom UI Components
│   │   └── AutoCompleteTextField.java     # Autocomplete search component
│   │
│   ├── config/                            # Spring Configuration
│   │   └── SpringFXMLLoader.java          # FXML Spring integration
│   │
│   └── view/                              # View Management
│       ├── StageManager.java              # Screen navigation
│       ├── FxmlView.java                  # FXML view enum
│       └── AlertNotification.java         # Alert dialogs
│
└── src/main/resources/
    ├── fxml/                              # FXML UI layouts
    │   ├── login.fxml                     # Login screen
    │   ├── home.fxml                      # Dashboard
    │   ├── createShope.fxml               # Shop registration
    │   ├── master/                        # Master data screens
    │   │   ├── MasterMenu.fxml
    │   │   ├── AddCategory.fxml
    │   │   ├── AddItem.fxml
    │   │   ├── AddCustomer.fxml
    │   │   ├── AddEmployee.fxml
    │   │   └── AddTable.fxml
    │   ├── transaction/                   # Transaction screens
    │   │   └── BillingFrame.fxml          # Billing interface
    │   ├── setting/                       # Settings screens
    │   │   ├── SettingMenu.fxml
    │   │   └── ApplicationSetting.fxml
    │   └── css/                           # Stylesheets
    │       ├── billing-material.css       # Billing screen styles
    │       └── category-material.css      # Category styles
    │
    ├── images/                            # Application icons & images
    └── application.properties             # Application configuration
```

## ✨ Features & Functionality

### 🔐 Authentication & Authorization
- **User Login** with username/password
- **Restaurant Selection** for multi-tenant support
- **Session Management** with user context
- **Role-based Access Control**
- **Password Encryption** using Spring Security

### 🏪 Shop/Restaurant Management
- **Restaurant Registration**
- Multi-restaurant support in single database
- Shop profile management
- Contact information management

### 👥 Master Data Management

#### Customer Management
- Add/Edit/Delete customers
- Customer search with autocomplete
- Store customer details (name, mobile, address)
- Customer history tracking

#### Employee Management
- Employee registration
- Role and designation assignment
- Contact information management
- Employee status tracking

#### Category Management
- Food category creation
- Category organization
- Active/Inactive status
- Category-based menu grouping

#### Item/Menu Management
- Menu item creation
- Item pricing
- Category association
- Item availability status

#### Table Management
- Table configuration
- Section-based organization
- Table capacity management
- Table status tracking

### 💰 Billing & POS System
- **Customer Search** with autocomplete (Marathi & English)
- **Table Selection** by section
- **Order Creation** and management
- **Item Selection** from menu
- **Bill Generation**
- **Payment Processing**
- **Receipt Printing**
- Real-time order tracking

### ⚙️ Application Settings
- **Custom Font Support** (Marathi - Kiran font)
- **Document Directory** configuration
- **System Preferences**
- **User Interface Settings**

### 🎨 UI/UX Features
- **Material Design** styling (Angular Material inspired)
- **Multi-language Support** (English & Marathi)
- **Custom Autocomplete** with intelligent search
- **Drag & Drop** support
- **Keyboard Navigation** (Arrow keys, Enter, Escape)
- **Responsive Layouts** with SplitPane
- **FontAwesome Icons** integration
- **Custom Kiran Font** for Marathi typing

## 🛠️ Technology Stack

### Core Framework
- **JavaFX 19.0.2.1** - Desktop UI framework
- **Spring Boot 3.4.5** - Application framework
- **Spring Data JPA** - Database access layer
- **Hibernate** - ORM implementation

### Database
- **MySQL 8.0+** - Relational database
- **Spring Data JPA** - Repository pattern
- **Direct Database Connection** (no external API)

### UI Components
- **JavaFX Controls** - Standard UI components
- **FontAwesomeFX** - Icon library
- **Custom Components** - AutoCompleteTextField
- **Material Design CSS** - Modern styling

### Additional Libraries
- **Lombok** - Reduce boilerplate code
- **Jackson** - JSON processing
- **Spring Security** - Password encryption
- **SLF4J + Logback** - Logging

## 🎯 Key Workflows

### 1. Application Startup
```
Main.java → Spring Boot Initialization → Database Connection → Login Screen
```

### 2. User Login
```
LoginController → AuthService → Database Verification → Session Creation → Home Screen
```

### 3. Billing Process
```
Select Table → Search Customer → Add Items → Calculate Total → Generate Bill → Payment → Print Receipt
```

### 4. Master Data Entry
```
Navigate to Master Menu → Select Module → Fill Form → Validate → Save to Database
```

## 🔧 Configuration

### Custom Font Setup
1. Place Marathi font file (Kiran.ttf) in a known location
2. Configure in Application Settings screen
3. Font path stored in `application_setting` table
4. Font loaded via `SessionService.getCustomFont()`

### Multi-language Support
- UI labels support both English and Marathi
- Input fields use Kiran font for Marathi typing
- Automatic font switching based on configuration

## 🚀 Running the Application

### Development Mode
```bash
mvn spring-boot:run
```

### Building Executable JAR
```bash
mvn clean package
java -jar target/hotel-frontend-1.0.0.jar
```

### Database Setup
1. Create MySQL database: `CREATE DATABASE hotel_db;`
2. Update `application.properties` with credentials
3. Run application - tables will be auto-created (ddl-auto=update)

## 📝 Development Guidelines

### Adding New Features

1. **Create Entity** (if needed)
   - Add JPA entity in `entity/` package
   - Define table structure and relationships

2. **Create Repository** (if needed)
   - Extend `JpaRepository` interface
   - Add custom query methods

3. **Create Service**
   - Business logic in `service/` package
   - Use `@Service` annotation
   - Inject repository via `@Autowired`

4. **Create FXML Layout**
   - Design UI in Scene Builder or manually
   - Place in appropriate `fxml/` subdirectory
   - Follow Material Design principles

5. **Create Controller**
   - Controller class in `controller/` package
   - Use `@Component` annotation
   - Inject services via `@Autowired`
   - Implement `Initializable` interface

6. **Update Navigation**
   - Add to `FxmlView` enum
   - Update menu in `HomeController`

### Styling Guidelines
- Use Material Design color palette
- Primary: #1976D2 (Material Blue 700)
- Accent: #FF9800 (Material Orange 500)
- Text Primary: #212121
- Text Secondary: #757575
- Follow existing CSS patterns in `fxml/css/`

## 📊 Database Schema

Key tables:
- `user` - User accounts
- `shop` - Restaurant details
- `employee` - Employee information
- `customer` - Customer records
- `category_master` - Food categories
- `item` - Menu items
- `tablemaster` - Table configuration
- `orders` - Order headers
- `order_items` - Order details
- `billing` - Billing records
- `application_setting` - App configuration

## 🐛 Troubleshooting

### Font Issues
- Ensure Kiran font path is correctly set in Application Settings
- Font must be accessible by the application
- Restart application after font configuration changes

### Database Connection Issues
- Verify MySQL service is running
- Check database credentials in `application.properties`
- Ensure database exists (`hotel_db`)
- Check firewall settings

### JavaFX Runtime Issues
- Ensure Java 17+ with JavaFX support
- Check JavaFX dependencies in `pom.xml`
- May need to add `--add-modules javafx.controls,javafx.fxml` to JVM args

## 📄 License

Proprietary - All rights reserved

## 👨‍💻 Support

For support and queries, contact the development team.

---

**Version**: 1.0.0
**Last Updated**: 2025
**Framework**: JavaFX + Spring Boot Desktop Application
