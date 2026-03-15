package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.entity.Shop;
import com.frontend.entity.User;
import com.frontend.service.AuthApiService;
import com.frontend.service.JwtService;
import com.frontend.service.SessionService;
import com.frontend.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API Controller for Web Application authentication.
 * Unlike the mobile AuthController, this does NOT check mobile access settings.
 * Works exactly like the desktop LoginController.
 * Only active in 'server' profile.
 */
@RestController
@RequestMapping("/api/v1/web")
@Tag(name = "Web Authentication", description = "Web application authentication endpoints (no mobile access check)")
@Profile("server")
public class WebAuthController {

    private static final Logger LOG = LoggerFactory.getLogger(WebAuthController.class);

    @Autowired
    private AuthApiService authApiService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private JwtService jwtService;

    /**
     * GET /api/v1/web/usernames
     * Get all usernames for login dropdown (no auth required)
     */
    @Operation(summary = "Get All Usernames", description = "Get usernames for login dropdown - no mobile access check")
    @GetMapping("/usernames")
    public ResponseEntity<ApiResponse> getAllUsernames() {
        try {
            List<String> usernames = authApiService.getAllUserNames();
            return ResponseEntity.ok(new ApiResponse("Usernames retrieved", true, usernames));
        } catch (Exception e) {
            LOG.error("Error retrieving usernames: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * GET /api/v1/web/shops
     * Get all shops/restaurants for login dropdown (no auth required)
     */
    @Operation(summary = "Get All Shops", description = "Get shops for login dropdown")
    @GetMapping("/shops")
    public ResponseEntity<ApiResponse> getAllShops() {
        try {
            List<Shop> shops = shopService.getAllShops();
            List<Map<String, Object>> shopList = new ArrayList<>();
            for (Shop shop : shops) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("shopId", shop.getShopId());
                s.put("restaurantName", shop.getRestaurantName());
                s.put("address", shop.getAddress());
                s.put("contactNumber", shop.getContactNumber());
                s.put("ownerName", shop.getOwnerName());
                shopList.add(s);
            }
            return ResponseEntity.ok(new ApiResponse("Shops retrieved", true, shopList));
        } catch (Exception e) {
            LOG.error("Error retrieving shops: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error: " + e.getMessage(), false));
        }
    }

    /**
     * POST /api/v1/web/login
     * Web login - works like desktop LoginController.
     * No mobile access check, includes shop selection, sets server session.
     */
    @Operation(summary = "Web Login", description = "Authenticate for web app - no mobile access restriction")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String shopIdStr = request.get("shopId");

        LOG.info("Web login attempt for user: {}", username);

        // Validate
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Username is required", false));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Password is required", false));
        }

        try {
            // Authenticate user (same as desktop)
            User user = authApiService.login(username.trim(), password.trim());

            // Get shop
            Shop shop = null;
            if (shopIdStr != null && !shopIdStr.isEmpty()) {
                shop = shopService.getShopById(Long.parseLong(shopIdStr)).orElse(null);
            }
            if (shop == null) {
                shop = shopService.getFirstShop().orElse(null);
            }

            // Set server session (same as desktop)
            if (shop != null) {
                sessionService.setUserSession(user, shop);
            }

            // Generate JWT token for web session
            List<String> features = List.of("web_full_access");
            String token = jwtService.generateToken(
                    user.getUsername(),
                    user.getRole(),
                    user.getId(),
                    user.getEmployee() != null ? user.getEmployee().getEmployeeId() : null,
                    user.getEmployee() != null ? user.getEmployee().getFullName() : null,
                    features
            );

            // Build response
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            data.put("userId", user.getId());
            if (user.getEmployee() != null) {
                data.put("employeeId", user.getEmployee().getEmployeeId());
                data.put("employeeName", user.getEmployee().getFullName());
            }
            if (shop != null) {
                data.put("shopId", shop.getShopId());
                data.put("restaurantName", shop.getRestaurantName());
            }

            LOG.info("Web login successful for user: {} (role: {})", username, user.getRole());
            return ResponseEntity.ok(new ApiResponse("Login successful", true, data));

        } catch (RuntimeException e) {
            LOG.error("Web login failed for user: {} - {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(e.getMessage(), false));
        }
    }

    /**
     * GET /api/v1/web/health
     * Health check (no mobile access check)
     */
    @Operation(summary = "Health Check", description = "Check server and database status")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        boolean dbConnected = authApiService.testConnection();
        if (dbConnected) {
            return ResponseEntity.ok(new ApiResponse("Server is running and database connected", true));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse("Server running but database connection failed", false));
        }
    }
}
