package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.dto.LoginRequest;
import com.frontend.dto.LoginResponse;
import com.frontend.entity.User;
import com.frontend.service.AuthApiService;
import com.frontend.service.JwtService;
import com.frontend.service.MobileAppSettingService;
import com.frontend.util.VersionUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for authentication endpoints
 * Only active in 'server' profile
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
@Profile("server")
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final AuthApiService authApiService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MobileAppSettingService mobileAppSettingService;

    public AuthController(AuthApiService authApiService) {
        this.authApiService = authApiService;
    }

    /**
     * POST /api/auth/login
     * Authenticate user and return login response with JWT token
     * Checks if mobile access is enabled in settings
     */
    @Operation(summary = "User Login", description = "Authenticate user with username and password. Returns JWT token for mobile app.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Mobile access disabled")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LOG.info("API Login attempt for user: {} (app version: {}, platform: {})",
                loginRequest.getUsername(), loginRequest.getAppVersion(), loginRequest.getPlatform());

        try {
            // Check if mobile access is enabled
            if (!mobileAppSettingService.isMobileAccessEnabled()) {
                LOG.warn("Mobile access is disabled. Login rejected for user: {}", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.mobileAccessDisabled());
            }

            // Check app version
            String minAppVersion = mobileAppSettingService.getSettingValue(MobileAppSettingService.MOBILE_APP_VERSION);
            String clientAppVersion = loginRequest.getAppVersion();
            boolean forceUpdate = mobileAppSettingService.getSettingBoolean(MobileAppSettingService.FORCE_UPDATE_ENABLED, false);

            if (minAppVersion != null && !minAppVersion.isEmpty()) {
                if (clientAppVersion == null || clientAppVersion.trim().isEmpty()) {
                    LOG.warn("No app version provided by client. User: {}", loginRequest.getUsername());
                    // If force update is enabled, reject login without version
                    if (forceUpdate) {
                        return ResponseEntity.status(HttpStatus.UPGRADE_REQUIRED)
                                .body(ApiResponse.appUpdateForced(minAppVersion));
                    }
                } else if (!VersionUtil.isVersionSufficient(clientAppVersion, minAppVersion)) {
                    LOG.warn("Client app version {} is below minimum required version {}. User: {}",
                            clientAppVersion, minAppVersion, loginRequest.getUsername());

                    if (forceUpdate) {
                        // Force update - reject login
                        return ResponseEntity.status(HttpStatus.UPGRADE_REQUIRED)
                                .body(ApiResponse.appUpdateForced(minAppVersion));
                    } else {
                        // Soft update - warn but allow login (include warning in response)
                        LOG.info("Allowing login with outdated app version (force update disabled)");
                    }
                }
            }

            // Validate request
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Username is required", false));
            }
            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Password is required", false));
            }

            // Authenticate using existing service
            User user = authApiService.login(loginRequest.getUsername(), loginRequest.getPassword());

            // Get enabled features for user's role
            List<String> features = mobileAppSettingService.getEnabledFeatureCodesForRole(user.getRole());

            // Get token expiration hours
            int expirationHours = mobileAppSettingService.getJwtExpirationHours();

            // Generate JWT token
            String token = jwtService.generateToken(
                    user.getUsername(),
                    user.getRole(),
                    user.getId(),
                    user.getEmployee() != null ? user.getEmployee().getEmployeeId() : null,
                    user.getEmployee() != null ? user.getEmployee().getFullName() : null,
                    features
            );

            // Build response
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(token);
            loginResponse.setUsername(user.getUsername());
            loginResponse.setRole(user.getRole());
            loginResponse.setUserId(user.getId());
            loginResponse.setFeatures(features);
            loginResponse.setTokenExpiresInHours(expirationHours);
            loginResponse.setTokenExpiresAt(System.currentTimeMillis() + (expirationHours * 60 * 60 * 1000L));

            if (user.getEmployee() != null) {
                loginResponse.setEmployeeId(user.getEmployee().getEmployeeId());
                loginResponse.setEmployeeName(user.getEmployee().getFullName());
            }

            // Check if app update is available (but not forced)
            boolean updateAvailable = false;
            if (minAppVersion != null && clientAppVersion != null &&
                    !VersionUtil.isVersionSufficient(clientAppVersion, minAppVersion)) {
                updateAvailable = true;
                loginResponse.setUpdateAvailable(true);
                loginResponse.setLatestVersion(minAppVersion);
            }

            LOG.info("API Login successful for user: {} (role: {}, features: {}, updateAvailable: {})",
                    loginRequest.getUsername(), user.getRole(), features.size(), updateAvailable);
            return ResponseEntity.ok(new ApiResponse("Login successful", true, loginResponse));

        } catch (RuntimeException e) {
            LOG.error("API Login failed for user: {} - {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(e.getMessage(), false, ApiResponse.ERROR_INVALID_CREDENTIALS));
        }
    }

    /**
     * GET /api/auth/health
     * Health check endpoint
     */
    @Operation(summary = "Health Check", description = "Check API and database connection status")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        boolean dbConnected = authApiService.testConnection();
        if (dbConnected) {
            return ResponseEntity.ok(new ApiResponse("API is running and database connected", true));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse("API is running but database connection failed", false));
        }
    }

    /**
     * GET /api/auth/usernames
     * Get all usernames for login screen dropdown
     */
    @Operation(summary = "Get All Usernames", description = "Retrieve all usernames for login screen dropdown")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usernames retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/usernames")
    public ResponseEntity<ApiResponse> getAllUsernames() {
        LOG.info("API request to get all usernames");

        try {
            List<String> usernames = authApiService.getAllUserNames();
            LOG.info("Retrieved {} usernames", usernames.size());
            return ResponseEntity.ok(new ApiResponse("Usernames retrieved successfully", true, usernames));
        } catch (Exception e) {
            LOG.error("Error retrieving usernames: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error retrieving usernames: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/auth/validate-token
     * Validate JWT token and return user info
     */
    @Operation(summary = "Validate Token", description = "Validate JWT token and return user info with features")
    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        LOG.info("API request to validate token");

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Invalid authorization header. Use 'Bearer <token>'", false));
            }

            String token = authHeader.substring(7);
            var claims = jwtService.validateToken(token);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("username", claims.getSubject());
            result.put("role", claims.get("role"));
            result.put("userId", claims.get("userId"));
            result.put("employeeId", claims.get("employeeId"));
            result.put("employeeName", claims.get("employeeName"));
            result.put("features", claims.get("features"));
            result.put("expiresAt", claims.getExpiration().getTime());
            result.put("isValid", true);

            LOG.info("Token validated for user: {}", claims.getSubject());
            return ResponseEntity.ok(new ApiResponse("Token is valid", true, result));

        } catch (ExpiredJwtException e) {
            LOG.warn("Token expired: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.tokenExpired());
        } catch (Exception e) {
            LOG.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.tokenInvalid());
        }
    }

    /**
     * GET /api/auth/mobile-config
     * Get mobile app configuration (version requirements, etc.)
     */
    @Operation(summary = "Get Mobile Config", description = "Get mobile app configuration like version requirements")
    @GetMapping("/mobile-config")
    public ResponseEntity<ApiResponse> getMobileConfig() {
        LOG.info("API request to get mobile config");

        try {
            java.util.Map<String, Object> config = new java.util.HashMap<>();
            config.put("mobileAccessEnabled", mobileAppSettingService.isMobileAccessEnabled());
            config.put("minAppVersion", mobileAppSettingService.getSettingValue(MobileAppSettingService.MOBILE_APP_VERSION));
            config.put("forceUpdate", mobileAppSettingService.getSettingBoolean(MobileAppSettingService.FORCE_UPDATE_ENABLED, false));
            config.put("tokenExpiryHours", mobileAppSettingService.getJwtExpirationHours());

            return ResponseEntity.ok(new ApiResponse("Mobile config retrieved", true, config));
        } catch (Exception e) {
            LOG.error("Error retrieving mobile config: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }
}
