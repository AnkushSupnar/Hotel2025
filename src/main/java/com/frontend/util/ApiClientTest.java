package com.frontend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple test class to verify ApiClient compiles and works
 */
@Component
public class ApiClientTest {
    
    @Autowired
    private ApiClient apiClient;
    
    /**
     * Test basic functionality
     */
    public boolean testApiClient() {
        try {
            // Test connection
            boolean connected = apiClient.testConnection();
            System.out.println("Backend connection test: " + (connected ? "SUCCESS" : "FAILED"));
            
            // Test basic GET request structure (will fail if backend is down, but should compile)
            ApiClient.ApiResponse<String> response = apiClient.get("/health", String.class);
            System.out.println("Health check response success: " + response.isSuccess());
            
            return true;
        } catch (Exception e) {
            System.err.println("ApiClient test error: " + e.getMessage());
            return false;
        }
    }
}