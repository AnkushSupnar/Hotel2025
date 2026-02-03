package com.frontend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontend.dto.ApiResponse;
import com.frontend.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT Authentication Filter
 * Intercepts all API requests and validates JWT token from Authorization header
 * Only active in 'server' profile
 */
@Component
@Profile("server")
@Order(1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/health",
            "/api/v1/auth/mobile-config",
            "/api/v1/auth/usernames",
            "/api/auth/login",
            "/api/auth/health",
            "/api/auth/mobile-config",
            "/api/auth/usernames",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/ws"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        LOG.debug("JWT Filter - Request: {} {}", method, requestPath);

        // Skip filter for non-API requests
        if (!requestPath.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip filter for public endpoints
        if (isPublicEndpoint(requestPath)) {
            LOG.debug("Public endpoint, skipping authentication: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOG.warn("Missing or invalid Authorization header for: {}", requestPath);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, ApiResponse.unauthorized());
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            var claims = jwtService.validateToken(token);

            // Token is valid - add user info to request attributes for use in controllers
            request.setAttribute("username", claims.getSubject());
            request.setAttribute("role", claims.get("role"));
            request.setAttribute("userId", claims.get("userId"));
            request.setAttribute("employeeId", claims.get("employeeId"));
            request.setAttribute("features", claims.get("features"));

            LOG.debug("Token validated for user: {}", claims.getSubject());

            // Continue with the request
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            LOG.warn("Token expired for request: {}", requestPath);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, ApiResponse.tokenExpired());
        } catch (Exception e) {
            LOG.error("Token validation failed: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, ApiResponse.tokenInvalid());
        }
    }

    /**
     * Check if the request path is a public endpoint
     */
    private boolean isPublicEndpoint(String requestPath) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(endpoint -> requestPath.startsWith(endpoint));
    }

    /**
     * Send error response as JSON
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status,
                                   ApiResponse apiResponse) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
