package com.frontend.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Helper class to access authenticated user information from JWT token
 * The JwtAuthenticationFilter adds these attributes to the request
 */
public class AuthenticatedUser {

    private final String username;
    private final String role;
    private final Long userId;
    private final Integer employeeId;
    private final List<String> features;

    private AuthenticatedUser(String username, String role, Long userId, Integer employeeId, List<String> features) {
        this.username = username;
        this.role = role;
        this.userId = userId;
        this.employeeId = employeeId;
        this.features = features;
    }

    /**
     * Extract authenticated user from request attributes
     * @param request the HTTP request
     * @return AuthenticatedUser or null if not authenticated
     */
    @SuppressWarnings("unchecked")
    public static AuthenticatedUser fromRequest(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        if (username == null) {
            return null;
        }

        Object role = request.getAttribute("role");
        Object userId = request.getAttribute("userId");
        Object employeeId = request.getAttribute("employeeId");
        Object features = request.getAttribute("features");

        return new AuthenticatedUser(
                (String) username,
                (String) role,
                userId != null ? ((Number) userId).longValue() : null,
                employeeId != null ? ((Number) employeeId).intValue() : null,
                (List<String>) features
        );
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public List<String> getFeatures() {
        return features;
    }

    /**
     * Check if user has a specific feature enabled
     */
    public boolean hasFeature(String featureCode) {
        return features != null && features.contains(featureCode);
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasRole(String... roles) {
        if (role == null) return false;
        for (String r : roles) {
            if (role.equalsIgnoreCase(r)) return true;
        }
        return false;
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", userId=" + userId +
                ", employeeId=" + employeeId +
                '}';
    }
}
