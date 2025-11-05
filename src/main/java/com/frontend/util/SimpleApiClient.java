package com.frontend.util;

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
 * Simple API client without Jackson dependencies
 * Use this if there are Jackson dependency issues
 */
@Component
public class SimpleApiClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(SimpleApiClient.class);
    
    private final RestTemplate restTemplate;
    private final String apiBaseUrl;
    
    public SimpleApiClient(@Value("${api.base.url:http://localhost:8080}") String apiBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.apiBaseUrl = apiBaseUrl;
    }
    
    /**
     * Make a GET request returning raw string
     */
    public String getRaw(String endpoint, String authToken) {
        try {
            String url = buildUrl(endpoint);
            HttpHeaders headers = createHeaders(authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            LOG.debug("GET request to: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );
            
            LOG.debug("GET response: {} - {}", response.getStatusCode(), url);
            return response.getBody();
            
        } catch (Exception e) {
            LOG.error("GET request failed: {} - {}", endpoint, e.getMessage());
            throw new RuntimeException(handleException(e));
        }
    }
    
    /**
     * Make a POST request with form data returning raw string
     */
    public String postFormRaw(String endpoint, Map<String, String> formData, String authToken) {
        try {
            String url = buildUrl(endpoint);
            HttpHeaders headers = createHeaders(authToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            formData.forEach(form::add);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
            
            LOG.debug("POST form request to: {} with data: {}", url, formData.keySet());
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            LOG.debug("POST form response: {} - {}", response.getStatusCode(), url);
            return response.getBody();
            
        } catch (Exception e) {
            LOG.error("POST form request failed: {} - {}", endpoint, e.getMessage());
            throw new RuntimeException(handleException(e));
        }
    }
    
    /**
     * Test connection to backend
     */
    public boolean testConnection() {
        try {
            String response = getRaw("/health", null);
            return response != null && !response.isEmpty();
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
     * Handle exceptions and return error message
     */
    private String handleException(Exception e) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException clientError = (HttpClientErrorException) e;
            if (clientError.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return "Authentication failed";
            } else if (clientError.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return "Invalid request";
            } else {
                return "Client error: " + clientError.getMessage();
            }
            
        } else if (e instanceof HttpServerErrorException) {
            return "Server error";
            
        } else if (e instanceof ResourceAccessException) {
            return "Cannot connect to server. Please ensure backend is running on port 8080.";
            
        } else {
            return "Unexpected error: " + e.getMessage();
        }
    }
}