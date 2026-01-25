package com.frontend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for JWT token generation and validation for mobile API
 */
@Service
public class JwtService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private MobileAppSettingService mobileAppSettingService;

    /**
     * Generate JWT token for mobile app login
     */
    public String generateToken(String username, String role, Long userId, Integer employeeId,
                                  String employeeName, List<String> features) {
        try {
            String secret = mobileAppSettingService.getJwtSecretKey();
            int expirationHours = mobileAppSettingService.getJwtExpirationHours();

            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Date now = new Date();
            Date expiry = new Date(now.getTime() + (expirationHours * 60 * 60 * 1000L));

            Map<String, Object> claims = new HashMap<>();
            claims.put("username", username);
            claims.put("role", role);
            claims.put("userId", userId);
            if (employeeId != null) {
                claims.put("employeeId", employeeId);
            }
            if (employeeName != null) {
                claims.put("employeeName", employeeName);
            }
            claims.put("features", features);

            String token = Jwts.builder()
                    .claims(claims)
                    .subject(username)
                    .issuedAt(now)
                    .expiration(expiry)
                    .signWith(key)
                    .compact();

            LOG.info("Generated JWT token for user: {} (expires in {} hours)", username, expirationHours);
            return token;

        } catch (Exception e) {
            LOG.error("Error generating JWT token for user: {}", username, e);
            throw new RuntimeException("Error generating token: " + e.getMessage());
        }
    }

    /**
     * Validate JWT token and return claims
     * @throws ExpiredJwtException if token is expired
     * @throws RuntimeException if token is invalid
     */
    public Claims validateToken(String token) throws ExpiredJwtException {
        String secret = mobileAppSettingService.getJwtSecretKey();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            LOG.debug("Token validated for user: {}", claims.getSubject());
            return claims;

        } catch (ExpiredJwtException e) {
            LOG.warn("Token expired: {}", e.getMessage());
            throw e;  // Re-throw to let filter handle it
        } catch (Exception e) {
            LOG.error("Error validating JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid token: " + e.getMessage());
        }
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Extract role from token
     */
    public String extractRole(String token) {
        return validateToken(token).get("role", String.class);
    }

    /**
     * Extract features from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractFeatures(String token) {
        return validateToken(token).get("features", List.class);
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get token expiration date
     */
    public Date getExpirationDate(String token) {
        return validateToken(token).getExpiration();
    }
}
