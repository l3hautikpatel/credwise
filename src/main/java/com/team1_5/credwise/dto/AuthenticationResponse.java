package com.team1_5.credwise.dto;

import java.util.Map;

public class AuthenticationResponse {
    private String token;
    private Map<String, Object> user;

    public AuthenticationResponse(String token, Map<String, Object> user) {
        this.token = token;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public void setUser(Map<String, Object> user) {
        this.user = user;
    }
}