package com.team1_5.credwise.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for JWT (JSON Web Token) operations
 *
 * Handles token generation, validation, and extraction of claims
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // JWT configuration properties
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // Default 24 hours
    private long jwtExpiration;

    /**
     * Generate a JWT token for a user
     *
     * @param userId Unique identifier of the user
     * @return Generated JWT token
     */
    public String generateToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userId.toString());
    }

    /**
     * Create a JWT token with claims
     *
     * @param claims Additional claims to include in the token
     * @param subject Subject of the token (usually user ID)
     * @return Generated JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            logger.error("Error creating JWT token", e);
            throw new RuntimeException("Error generating token", e);
        }
    }

    /**
     * Extract a specific claim from the token
     *
     * @param token JWT token
     * @param claimsResolver Function to extract specific claim
     * @return Extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract user ID from JWT token
     *
     * @param token JWT token
     * @return User ID extracted from the token
     */
    public Long extractUserId(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // Extract subject (user ID) from token
            String userIdString = extractClaim(token, Claims::getSubject);

            return Long.parseLong(userIdString);
        } catch (Exception e) {
            logger.error("Error extracting user ID from token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Check if token is expired
     *
     * @param token JWT token
     * @return Boolean indicating if token is expired
     */
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extract expiration date from token
     *
     * @param token JWT token
     * @return Expiration date of the token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token
     * @param userId User ID to validate against
     * @return Boolean indicating token validity
     */
    public Boolean validateToken(String token, Long userId) {
        try {
            final Long extractedUserId = extractUserId(token);
            return (extractedUserId.equals(userId) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.warn("Token validation failed", e);
            return false;
        }
    }

    /**
     * Extract all claims from the token
     *
     * @param token JWT token
     * @return Claims extracted from the token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Get signing key for token signature
     *
     * @return Signing key
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Refresh a JWT token
     *
     * @param token Existing token
     * @return New refreshed token
     */
    public String refreshToken(String token) {
        try {
            // Extract claims from existing token
            Claims claims = extractAllClaims(token);

            // Create a new token with same claims and updated expiration
            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            logger.error("Error refreshing JWT token", e);
            throw new RuntimeException("Error refreshing token", e);
        }
    }
}