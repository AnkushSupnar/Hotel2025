package com.frontend.dto;

/**
 * DTO for login response from backend API
 */
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private Integer employeeId;
    private String employeeName;

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
}