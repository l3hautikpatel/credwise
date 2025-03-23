package com.team1_5.credwise.dto;

public class LoanApplicationResponse {
    private String applicationId;
    private String message;
    private String status;

    // Constructor
    public LoanApplicationResponse() {}

    public LoanApplicationResponse(String applicationId, String message, String status) {
        this.applicationId = applicationId;
        this.message = message;
        this.status = status;
    }

    // Getters and Setters
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}