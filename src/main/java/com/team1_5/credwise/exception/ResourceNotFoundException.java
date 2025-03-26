// src/main/java/com/team1_5/credwise/exception/ResourceNotFoundException.java
package com.team1_5.credwise.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}