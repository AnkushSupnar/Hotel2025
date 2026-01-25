package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.dto.LoginRequest;
import com.frontend.dto.LoginResponse;
import com.frontend.entity.User;
import com.frontend.service.AuthApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API Controller for authentication endpoints
 * Only active in 'server' profile
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
@Profile("server")
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final AuthApiService authApiService;

    
    public AuthController(AuthApiService authApiService) {
        this.authApiService = authApiService;
    }

    /**
     * POST /api/auth/login
     * Authenticate user and return login response with token
     */
    @Operation(summary = "User Login", description = "Authenticate user with username and password")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest loginRequest) {
        LOG.info("API Login attempt for user: {}", loginRequest.getUsername());

        try {
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

            // Generate a simple token (for production, use JWT)
            String token = UUID.randomUUID().toString();

            // Build response
            LoginResponse loginResponse;
            if (user.getEmployee() != null) {
                loginResponse = new LoginResponse(
                        token,
                        user.getUsername(),
                        user.getRole(),
                        user.getEmployee().getEmployeeId(),
                        user.getEmployee().getFullName()
                );
            } else {
                loginResponse = new LoginResponse(token, user.getUsername(), user.getRole());
            }

            LOG.info("API Login successful for user: {}", loginRequest.getUsername());
            return ResponseEntity.ok(new ApiResponse("Login successful", true, loginResponse));

        } catch (RuntimeException e) {
            LOG.error("API Login failed for user: {} - {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(e.getMessage(), false));
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
}
