package com.frontend.dto;

import java.util.List;

/**
 * DTO for login response from backend API
 */
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private Long userId;
    private Integer employeeId;
    private String employeeName;
    private List<String> features;      // Enabled features for mobile app
    private Long tokenExpiresAt;        // Token expiration timestamp (milliseconds)
    private Integer tokenExpiresInHours; // Token expiration in hours
    private Boolean updateAvailable;    // True if app update is available
    private String latestVersion;       // Latest app version available

    public LoginResponse() {
    }

    public LoginResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    public LoginResponse(String token, String username, String role, Integer employeeId, String employeeName) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public Long getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Long tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public Integer getTokenExpiresInHours() {
        return tokenExpiresInHours;
    }

    public void setTokenExpiresInHours(Integer tokenExpiresInHours) {
        this.tokenExpiresInHours = tokenExpiresInHours;
    }

    public Boolean getUpdateAvailable() {
        return updateAvailable;
    }

    public void setUpdateAvailable(Boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }
}