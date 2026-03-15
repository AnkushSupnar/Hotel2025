# ApiClient Usage Guide

The `ApiClient` is a common utility class for making HTTP requests to the backend API. It provides standardized error handling, logging, and response parsing.

## Basic Usage

### 1. Inject ApiClient in your service:

```java
@Service
public class YourService {
    
    private final ApiClient apiClient;
    private final SessionService sessionService;
    
    @Autowired
    public YourService(ApiClient apiClient, SessionService sessionService) {
        this.apiClient = apiClient;
        this.sessionService = sessionService;
    }
}
```

### 2. Make API calls:

#### GET Request (without authentication):
```java
ApiClient.ApiResponse<List> response = apiClient.get("/public/data", List.class);
if (response.isSuccess()) {
    List data = response.getData();
    // Process data
} else {
    // Handle error
    String error = response.getErrorMessage();
}
```

#### GET Request (with authentication):
```java
String authToken = sessionService.getJwtToken();
ApiClient.ApiResponse<Map> response = apiClient.get("/protected/data", authToken, Map.class);
```

#### POST Request with JSON body:
```java
Map<String, Object> requestData = new HashMap<>();
requestData.put("name", "Example");
requestData.put("value", 123);

String authToken = sessionService.getJwtToken();
ApiClient.ApiResponse<Map> response = apiClient.post(
    "/data", 
    requestData, 
    authToken, 
    Map.class
);
```

#### POST Request with form data:
```java
Map<String, String> formData = new HashMap<>();
formData.put("username", "user");
formData.put("password", "pass");

ApiClient.ApiResponse<LoginResponse> response = apiClient.postForm(
    "/auth/login", 
    formData, 
    LoginResponse.class
);
```

#### PUT Request:
```java
Map<String, Object> updateData = new HashMap<>();
updateData.put("name", "Updated Name");

String authToken = sessionService.getJwtToken();
ApiClient.ApiResponse<Map> response = apiClient.put(
    "/items/123", 
    updateData, 
    authToken, 
    Map.class
);
```

#### DELETE Request:
```java
String authToken = sessionService.getJwtToken();
ApiClient.ApiResponse<Map> response = apiClient.delete(
    "/items/123", 
    authToken, 
    Map.class
);
```

## Error Handling

The ApiClient automatically handles common HTTP errors:

- **401 Unauthorized**: "Authentication failed"
- **400 Bad Request**: "Invalid request"
- **5xx Server Errors**: "Server error"
- **Connection Issues**: "Cannot connect to server"

### Check for errors:
```java
ApiClient.ApiResponse<Map> response = apiClient.get("/data", Map.class);

if (response.hasError()) {
    // Show error to user
    alertNotification.showError(response.getErrorMessage());
    return;
}

// Process successful response
Map data = response.getData();
```

## URL Building

The ApiClient automatically builds complete URLs:
- Base URL: `${api.base.url}` from application.properties
- API prefix: `/api` is automatically added
- Endpoint: Your provided endpoint

Example:
- Input: `/auth/login`
- Result: `http://localhost:8080/api/auth/login`

## Authentication

For protected endpoints, pass the JWT token:

```java
String authToken = sessionService.getJwtToken();
ApiClient.ApiResponse<Map> response = apiClient.get("/protected", authToken, Map.class);
```

The token is automatically formatted as: `Authorization: Bearer <token>`

## Connection Testing

Test backend connectivity:

```java
if (apiClient.testConnection()) {
    // Backend is available
} else {
    // Backend is down
    alertNotification.showError("Backend server is not available");
}
```

## Complete Example

```java
@Service
public class MenuService {
    
    private final ApiClient apiClient;
    private final SessionService sessionService;
    
    @Autowired
    public MenuService(ApiClient apiClient, SessionService sessionService) {
        this.apiClient = apiClient;
        this.sessionService = sessionService;
    }
    
    public List<Map<String, Object>> getMenuItems() {
        String authToken = sessionService.getJwtToken();
        
        ApiClient.ApiResponse<List> response = apiClient.get(
            "/menu/items", 
            authToken, 
            List.class
        );
        
        if (response.isSuccess()) {
            return response.getData();
        } else {
            throw new RuntimeException(response.getErrorMessage());
        }
    }
    
    public boolean createMenuItem(String name, double price) {
        Map<String, Object> menuItem = new HashMap<>();
        menuItem.put("name", name);
        menuItem.put("price", price);
        
        String authToken = sessionService.getJwtToken();
        
        ApiClient.ApiResponse<Map> response = apiClient.post(
            "/menu/items", 
            menuItem, 
            authToken, 
            Map.class
        );
        
        return response.isSuccess();
    }
}
```

## Benefits

1. **Consistent Error Handling**: All API calls have the same error handling pattern
2. **Automatic Authorization**: JWT tokens are handled automatically
3. **Logging**: All requests and responses are logged for debugging
4. **Type Safety**: Generic response types for compile-time safety
5. **URL Management**: Automatic URL building with proper prefixes
6. **Connection Testing**: Built-in backend connectivity testing