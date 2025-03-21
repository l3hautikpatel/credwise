package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.UserUpdateRequest;
import com.team1_5.credwise.exception.UserNotFoundException;
import com.team1_5.credwise.model.User;
import com.team1_5.credwise.repository.UserRepository;
import com.team1_5.credwise.util.PasswordUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Comprehensive service for user management
 *
 * Provides methods for user creation, update, retrieval, and related operations
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * Create a new user
     *
     * @param user User entity to create
     * @return Created user
     */
    @Transactional
    public User createUser(User user) {
        // Validate user doesn't already exist
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            throw new RuntimeException("Phone number already exists");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default status and creation timestamp
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        // Save user
        User savedUser = userRepository.save(user);

        // Log user creation event
        auditLogService.logEvent(
                AuditLogService.AuditEventType.USER_REGISTRATION,
                "New user created",
                java.util.Map.of("userId", savedUser.getId(), "email", savedUser.getEmail())
        );

        return savedUser;
    }

    /**
     * Update existing user profile
     *
     * @param userId User ID
     * @param updateRequest User update request
     * @return Updated user
     */
    @Transactional
    public User updateUserProfile(Long userId, UserUpdateRequest updateRequest) {
        // Find existing user
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Update user details
        updateUserDetails(existingUser, updateRequest);

        // Save updated user
        User updatedUser = userRepository.save(existingUser);

        // Log user update event
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "User profile updated",
                java.util.Map.of("userId", userId, "updatedFields", getUpdatedFields(updateRequest))
        );

        return updatedUser;
    }

    /**
     * Update user details
     *
     * @param user Existing user
     * @param updateRequest Update request
     */
    private void updateUserDetails(User user, UserUpdateRequest updateRequest) {
        // Update basic profile information
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }

        // Update email with validation
        if (updateRequest.getEmail() != null) {
            // Check if email is already in use
            Optional<User> existingUserWithEmail = userRepository.findByEmail(updateRequest.getEmail());
            if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(user.getId())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(updateRequest.getEmail());
        }

        // Update phone number with validation
        if (updateRequest.getPhoneNumber() != null) {
            // Check if phone number is already in use
            Optional<User> existingUserWithPhone = userRepository.findByPhoneNumber(updateRequest.getPhoneNumber());
            if (existingUserWithPhone.isPresent() && !existingUserWithPhone.get().getId().equals(user.getId())) {
                throw new RuntimeException("Phone number already in use");
            }
            user.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        // Update password if provided
        if (updateRequest.getNewPassword() != null) {
            // Validate current password
            if (!passwordEncoder.matches(updateRequest.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }

            // Encode and set new password
            user.setPassword(passwordEncoder.encode(updateRequest.getNewPassword()));
        }
    }

    /**
     * Change user status
     *
     * @param userId User ID
     * @param status New user status
     */
    @Transactional
    public void changeUserStatus(Long userId, User.UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setStatus(status);
        userRepository.save(user);

        // Log status change event
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "User status changed",
                java.util.Map.of("userId", userId, "newStatus", status)
        );
    }

    /**
     * Delete user account
     *
     * @param userId User ID to delete
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Soft delete or hard delete based on requirements
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);

        // Log deletion event
        auditLogService.logEvent(
                AuditLogService.AuditEventType.SYSTEM_ERROR,
                "User account deleted",
                java.util.Map.of("userId", userId)
        );
    }

    /**
     * Get user by ID
     *
     * @param userId User ID
     * @return User details
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * Get updated fields for logging
     *
     * @param updateRequest User update request
     * @return Map of updated fields
     */
    private java.util.Map<String, Object> getUpdatedFields(UserUpdateRequest updateRequest) {
        java.util.Map<String, Object> updatedFields = new java.util.HashMap<>();

        if (updateRequest.getFirstName() != null) updatedFields.put("firstName", true);
        if (updateRequest.getLastName() != null) updatedFields.put("lastName", true);
        if (updateRequest.getEmail() != null) updatedFields.put("email", true);
        if (updateRequest.getPhoneNumber() != null) updatedFields.put("phoneNumber", true);
        if (updateRequest.getNewPassword() != null) updatedFields.put("password", true);

        return updatedFields;
    }
}