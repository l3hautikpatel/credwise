package com.team1_5.credwise.exception;

/**
 * Exception thrown when a user is not found in the system
 */
public class UserNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }

    public UserNotFoundException(String field, String value) {
        super("User not found with " + field + ": " + value);
    }
}