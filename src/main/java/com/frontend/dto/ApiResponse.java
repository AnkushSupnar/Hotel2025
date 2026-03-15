package com.frontend.dto;

/**
 * Generic DTO for API responses from backend
 */
public class ApiResponse {
    private String message;
    private boolean success;
    private Object data;
    private String errorCode;  // Error code for client-side handling

    // Error codes constants
    public static final String ERROR_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String ERROR_TOKEN_INVALID = "TOKEN_INVALID";
    public static final String ERROR_APP_UPDATE_REQUIRED = "APP_UPDATE_REQUIRED";
    public static final String ERROR_APP_UPDATE_FORCED = "APP_UPDATE_FORCED";
    public static final String ERROR_MOBILE_ACCESS_DISABLED = "MOBILE_ACCESS_DISABLED";
    public static final String ERROR_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ERROR_UNAUTHORIZED = "UNAUTHORIZED";

    public ApiResponse() {
    }

    public ApiResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public ApiResponse(String message, boolean success, Object data) {
        this.message = message;
        this.success = success;
        this.data = data;
    }

    public ApiResponse(String message, boolean success, String errorCode) {
        this.message = message;
        this.success = success;
        this.errorCode = errorCode;
    }

    public ApiResponse(String message, boolean success, Object data, String errorCode) {
        this.message = message;
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    // Static factory methods for common error responses
    public static ApiResponse tokenExpired() {
        return new ApiResponse("Your session has expired. Please login again.", false, ERROR_TOKEN_EXPIRED);
    }

    public static ApiResponse tokenInvalid() {
        return new ApiResponse("Invalid authentication token. Please login again.", false, ERROR_TOKEN_INVALID);
    }

    public static ApiResponse appUpdateRequired(String minVersion) {
        return new ApiResponse("Please update your app to version " + minVersion + " or higher.", false, ERROR_APP_UPDATE_REQUIRED);
    }

    public static ApiResponse appUpdateForced(String minVersion) {
        return new ApiResponse("A critical update is required. Please update your app to version " + minVersion + " to continue.", false, ERROR_APP_UPDATE_FORCED);
    }

    public static ApiResponse mobileAccessDisabled() {
        return new ApiResponse("Mobile app access is currently disabled. Please contact administrator.", false, ERROR_MOBILE_ACCESS_DISABLED);
    }

    public static ApiResponse unauthorized() {
        return new ApiResponse("You are not authorized to access this resource. Please login.", false, ERROR_UNAUTHORIZED);
    }
}