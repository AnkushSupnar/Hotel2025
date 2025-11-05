package com.frontend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Common API client utility for making HTTP requests to backend
 * Provides standardized error handling, logging, and response parsing
 */
@Component
public class ApiClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private String apiBaseUrl;
    
    public ApiClient(@Value("${api.base.url:http://localhost:8080}") String apiBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiBaseUrl = apiBaseUrl;
    }
    
    /**
     * Make a GET request
     */
    public <T> ApiResponse<T> get(String endpoint, Class<T> responseType) {
        return get(endpoint, null, responseType);
    }
    
    /**
     * Make a GET request with authorization header
     */
    public <T> ApiResponse<T> get(String endpoint, String authToken, Class<T> responseType) {
        try {
            String url = buildUrl(endpoint);
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            LOG.debug("GET request to: {}", url);
            
            ResponseEntity<T> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, responseType
            );
            
            LOG.debug("GET response: {} - {}", response.getStatusCode(), url);
            return ApiResponse.success(response.getBody());
            
        } catch (Exception e) {
            LOG.error("GET request failed: {} - {}", endpoint, e.getMessage());
            return handleException(e);
        }
    }
    
    /**
     * Make a POST request with JSON body
     */
    public <T, R> ApiResponse<R> post(String endpoint, T requestBody, Class<R> responseType) {
        return post(endpoint, requestBody, null, responseType);
    }
    
    /**
     * Make a POST request with JSON body and authorization
     */
    public <T, R> ApiResponse<R> post(String endpoint, T requestBody, String authToken, Class<R> responseType) {
        try {
            String url = buildUrl(endpoint);
            HttpHeaders headers = createHeaders(authToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);
            
            LOG.debug("POST request to: {} with body: {}", url, requestBody);
            
            ResponseEntity<R> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, responseType
            );
            
            LOG.debug("POST response: {} - {}", response.getStatusCode(), url);
            return ApiResponse.success(response.getBody());
            
        } catch (Exception e) {
            LOG.error("POST request failed: {} - {}", endpoint, e.getMessage());
            return handleException(e);
        }
    }
    
    /**
     * Make a POST request with form data
     */
    public <R> ApiResponse<R> postForm(String endpoint, Map<String, String> formData, Class<R> responseType) {
        return postForm(endpoint, formData, null, responseType);
    }
    
    /**
     * Make a POST request with form data and authorization
     */
    public <R> ApiResponse<R> postForm(String endpoint, Map<String, String> formData, String authToken, Class<R> responseType) {
        try {
            String url = buildUrl(endpoint);
            HttpHeaders headers = createHeaders(authToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            formData.forEach(form::add);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
            
            LOG.debug("POST form request to: {} with data: {}", url, formData.keySet());
            
            ResponseEntity<R> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, responseType
            );
            
            LOG.debug("POST form response: {} - {}", response.getStatusCode(), url);
            return ApiResponse.success(response.getBody());
            
        } catch (Exception e) {
            LOG.error("POST form request failed: {} - {}", endpoint, e.getMessage());
            return handleException(e);
        }
    }
    
    /**
     * Make a PUT request with JSON body
     */
    public <T, R> ApiResponse<R> put(String endpoint, T requestBody, String authToken, Class<R> responseType) {
        try {
            String url = buildUrl(endpoint);
            HttpHeaders headers = createHeaders(authToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);
            
            LOG.debug("PUT request to: {} with body: {}", url, requestBody);
            
            ResponseEntity<R> response = restTemplate.exchange(
                url, HttpMethod.PUT, entity, responseType
            );
            
            LOG.debug("PUT response: {} - {}", response.getStatusCode(), url);
            return ApiResponse.success(response.getBody());
            
        } catch (Exception e) {
            LOG.error("PUT request failed: {} - {}", endpoint, e.getMessage());
            return handleException(e);
        }
    }
    
    /**
     * Make a DELETE request
     */
    public <R> ApiResponse<R> delete(String endpoint, String authToken, Class<R> responseType) {
        try {
            String url = buildUrl(endpoint);
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            LOG.debug("DELETE request to: {}", url);
            
            ResponseEntity<R> response = restTemplate.exchange(
                url, HttpMethod.DELETE, entity, responseType
            );
            
            LOG.debug("DELETE response: {} - {}", response.getStatusCode(), url);
            return ApiResponse.success(response.getBody());
            
        } catch (Exception e) {
            LOG.error("DELETE request failed: {} - {}", endpoint, e.getMessage());
            return handleException(e);
        }
    }
    
    /**
     * Test connection to backend
     */
    public boolean testConnection() {
        try {
            ApiResponse<Map> response = get("/health", Map.class);
            return response.isSuccess();
        } catch (Exception e) {
            LOG.error("Backend connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Build complete URL from endpoint
     */
    private String buildUrl(String endpoint) {
        String cleanEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return apiBaseUrl + "/api" + cleanEndpoint;
    }
    
    /**
     * Create HTTP headers with optional authorization
     */
    private HttpHeaders createHeaders(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        
        if (authToken != null && !authToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + authToken);
        }
        
        return headers;
    }
    
    /**
     * Handle exceptions and convert to ApiResponse
     */
    @SuppressWarnings("unchecked")
    private <T> ApiResponse<T> handleException(Exception e) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException clientError = (HttpClientErrorException) e;
            String errorMessage = parseErrorMessage(clientError.getResponseBodyAsString());
            
            if (clientError.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return ApiResponse.error("Authentication failed: " + errorMessage);
            } else if (clientError.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return ApiResponse.error("Invalid request: " + errorMessage);
            } else {
                return ApiResponse.error("Client error: " + errorMessage);
            }
            
        } else if (e instanceof HttpServerErrorException) {
            HttpServerErrorException serverError = (HttpServerErrorException) e;
            String errorMessage = parseErrorMessage(serverError.getResponseBodyAsString());
            return ApiResponse.error("Server error: " + errorMessage);
            
        } else if (e instanceof ResourceAccessException) {
            return ApiResponse.error("Cannot connect to server. Please ensure backend is running on port 8080.");
            
        } else {
            return ApiResponse.error("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Parse error message from response body
     */
    @SuppressWarnings("unchecked")
    private String parseErrorMessage(String responseBody) {
        try {
            Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
            Object message = errorMap.get("message");
            return message != null ? message.toString() : "Unknown error";
        } catch (Exception e) {
            return responseBody != null && !responseBody.isEmpty() ? responseBody : "Unknown error";
        }
    }
    
    /**
     * Generic API Response wrapper
     */
    public static class ApiResponse<T> {
        private final boolean success;
        private final T data;
        private final String errorMessage;
        
        private ApiResponse(boolean success, T data, String errorMessage) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
        }
        
        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, data, null);
        }
        
        public static <T> ApiResponse<T> error(String errorMessage) {
            return new ApiResponse<>(false, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public T getData() {
            return data;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public boolean hasError() {
            return !success;
        }
    }
    
    /**
     * Update the base URL for API calls
     */
    public void updateBaseUrl(String newBaseUrl) {
        LOG.debug("Updating API base URL from {} to {}", this.apiBaseUrl, newBaseUrl);
        this.apiBaseUrl = newBaseUrl;
    }
    
    /**
     * Convenience method for JSON POST requests (alias for post)
     */
    public <T, R> ApiResponse<R> postJson(String endpoint, T requestBody, String authToken, Class<R> responseType) {
        return post(endpoint, requestBody, authToken, responseType);
    }
    
    /**
     * Convenience method for JSON PUT requests (alias for put)
     */
    public <T, R> ApiResponse<R> putJson(String endpoint, T requestBody, String authToken, Class<R> responseType) {
        return put(endpoint, requestBody, authToken, responseType);
    }
}