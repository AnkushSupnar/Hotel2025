# Hotel Management Frontend

JavaFX desktop application with Spring Boot for the Hotel Management System.

## Quick Start

```bash
# Navigate to frontend directory
cd hotel-frontend

# Run the application
mvn spring-boot:run
```

**Application will start on**: http://localhost:8081

## Prerequisites

- Java 17+
- Maven 3.6+
- Backend server running on http://localhost:8080

## Project Structure

```
src/main/java/com/gurukrupa/
├── controller/          # FXML controllers
├── service/            # API client services
├── config/             # Spring configuration
├── view/               # View management
└── common/             # Common utilities

src/main/resources/
├── fxml/               # FXML layout files
├── css/                # Stylesheets
├── images/             # Icons and images
└── application.properties
```

## Features

### Main Dashboard
- Real-time hotel statistics
- Quick navigation to all modules
- User information display

### Billing System
- Create and manage orders
- Generate bills with tax calculation
- Print receipts

### Menu Management
- Add/edit menu items
- Category management
- Price updates

### Transaction Management
- Order tracking
- Payment processing
- Transaction history

### Reports
- Daily sales reports
- Revenue analytics
- Custom reports

## Configuration

### API Configuration
```properties
# application.properties
api.base.url=http://localhost:8080
server.port=8081
spring.main.web-application-type=none
```

## UI Structure

### Navigation Menu
- Dashboard
- Billing
- Transactions
- Master Data
- Reports
- Settings
- Support

### FXML Controllers
Each controller handles specific UI functionality and communicates with backend via API services.

## Development

### Adding New Features
1. Create FXML layout in `src/main/resources/fxml/`
2. Create controller class with `@Component` annotation
3. Add API service methods for backend communication
4. Update navigation in HomeController

### Styling
- CSS files located in `src/main/resources/fxml/css/`
- Follow existing color scheme and patterns
- Use FontAwesome icons for consistency