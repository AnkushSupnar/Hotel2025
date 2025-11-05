package com.frontend.service;

import com.frontend.dto.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing user session and JWT token storage
 */
@Service
public class SessionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(SessionService.class);
    
    private LoginResponse currentUser;
    private String jwtToken;
    
    /**
     * Store user session after successful login
     */
    public void setUserSession(LoginResponse loginResponse) {
        LOG.debug("Setting user session for: {}", loginResponse != null ? loginResponse.getUsername() : "null");
        LOG.debug("Login response token: {}", loginResponse != null && loginResponse.getToken() != null ? "***" + loginResponse.getToken().substring(Math.max(0, loginResponse.getToken().length() - 10)) : "null");
        
        this.currentUser = loginResponse;
        this.jwtToken = loginResponse != null ? loginResponse.getToken() : null;
        
        LOG.debug("Session set - isLoggedIn: {}", isLoggedIn());
    }
    
    /**
     * Get current logged-in user
     */
    public LoginResponse getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Get JWT token for API requests
     */
    public String getJwtToken() {
        LOG.debug("Getting JWT token: {}", jwtToken != null ? "***" + jwtToken.substring(Math.max(0, jwtToken.length() - 10)) : "null");
        return jwtToken;
    }
    
    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        boolean loggedIn = currentUser != null && jwtToken != null && !jwtToken.isEmpty();
        LOG.debug("Checking isLoggedIn: currentUser={}, jwtToken={}, result={}", 
                  currentUser != null ? currentUser.getUsername() : "null",
                  jwtToken != null ? "***" + jwtToken.substring(Math.max(0, jwtToken.length() - 10)) : "null",
                  loggedIn);
        return loggedIn;
    }
    
    /**
     * Get current username
     */
    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
    
    /**
     * Get current user role
     */
    public String getCurrentUserRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }
    
    /**
     * Clear user session (logout)
     */
    public void clearSession() {
        this.currentUser = null;
        this.jwtToken = null;
    }
    
    /**
     * Get authorization header for API requests
     */
    public String getAuthorizationHeader() {
        return jwtToken != null ? "Bearer " + jwtToken : null;
    }
}