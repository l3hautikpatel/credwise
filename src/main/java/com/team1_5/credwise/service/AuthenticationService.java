package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.AuthenticationRequest;
import com.team1_5.credwise.dto.AuthenticationResponse;
import com.team1_5.credwise.dto.RegisterRequest;
import com.team1_5.credwise.exception.UserNotFoundException;
import com.team1_5.credwise.model.User;
import com.team1_5.credwise.repository.UserRepository;
import com.team1_5.credwise.util.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Comprehensive Authentication Service
 *
 * Handles user authentication, registration, and related security operations
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AuthenticationManager authenticationManager,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
    }

    /**
     * User registration process
     *
     * @param request Registration request details
     * @return Authentication response with JWT token
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already exists");
        }

        // Create new user
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser.getId());

        // Log registration event
        auditLogService.logEvent(
                AuditLogService.AuditEventType.USER_REGISTRATION,
                "New user registered",
                Map.of("userId", savedUser.getId(), "email", savedUser.getEmail())
        );

        // Return authentication response
        return new AuthenticationResponse(
                token,
                mapUserToDto(savedUser)
        );
    }

    /**
     * User authentication process
     *
     * @param request Authentication request details
     * @return Authentication response with JWT token
     */
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Find user
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId());

            // Log login event
            auditLogService.logEvent(
                    AuditLogService.AuditEventType.LOGIN,
                    "User logged in",
                    Map.of("userId", user.getId(), "email", user.getEmail())
            );

            // Return authentication response
            return new AuthenticationResponse(
                    token,
                    mapUserToDto(user)
            );

        } catch (Exception e) {
            // Log authentication failure
            auditLogService.logEvent(
                    AuditLogService.AuditEventType.SYSTEM_ERROR,
                    "Authentication failed",
                    Map.of("email", request.getEmail(), "error", e.getMessage())
            );
            throw e;
        }
    }

    /**
     * Refresh JWT token
     *
     * @param token Existing JWT token
     * @return New JWT token
     */
    public AuthenticationResponse refreshToken(String token) {
        try {
            // Extract user ID from existing token
            Long userId = jwtUtil.extractUserId(token);

            // Find user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // Generate new token
            String newToken = jwtUtil.generateToken(userId);

            // Log token refresh event
            auditLogService.logEvent(
                    AuditLogService.AuditEventType.SYSTEM_ERROR,
                    "JWT token refreshed",
                    Map.of("userId", userId)
            );

            // Return new authentication response
            return new AuthenticationResponse(
                    newToken,
                    mapUserToDto(user)
            );

        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Logout user
     *
     * @param userId User ID
     */
    public void logout(Long userId) {
        // Log logout event
        auditLogService.logEvent(
                AuditLogService.AuditEventType.LOGOUT,
                "User logged out",
                Map.of("userId", userId)
        );

        // Clear security context
        SecurityContextHolder.clearContext();
    }

    /**
     * Map user to DTO for response
     *
     * @param user User entity
     * @return User DTO
     */
    private Map<String, Object> mapUserToDto(User user) {
        Map<String, Object> userDto = new HashMap<>();
        userDto.put("id", user.getId());
        userDto.put("firstName", user.getFirstName());
        userDto.put("lastName", user.getLastName());
        userDto.put("email", user.getEmail());
        userDto.put("phoneNumber", user.getPhoneNumber());
        return userDto;
    }

    /**
     * Password reset request
     *
     * @param email User's email
     * @return Boolean indicating success
     */
    @Transactional
    public boolean requestPasswordReset(String email) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Generate and send password reset token
        String resetToken = generatePasswordResetToken();

        // Log password reset request
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "Password reset requested",
                Map.of("userId", user.getId(), "email", email)
        );

        // In a real-world scenario, you would:
        // 1. Save reset token in database
        // 2. Send email with reset link
        // 3. Set token expiration

        return true;
    }

    /**
     * Generate password reset token
     *
     * @return Generated reset token
     */
    private String generatePasswordResetToken() {
        // Implement secure token generation logic
        return java.util.UUID.randomUUID().toString();
    }
}