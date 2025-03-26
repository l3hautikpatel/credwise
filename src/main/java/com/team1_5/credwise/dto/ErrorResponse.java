package com.team1_5.credwise.dto;

public class ErrorResponse {
    private String message;
    private int statusCode;

    public ErrorResponse(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getMessage() { return message; }
    public int getStatusCode() { return statusCode; }
}
