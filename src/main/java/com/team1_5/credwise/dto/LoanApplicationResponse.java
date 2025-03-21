package com.team1_5.credwise.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Loan Application Response
 *
 * Represents the response after submitting a loan application
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
public class LoanApplicationResponse {
    // Unique identifier for the loan application
    private String applicationId;

    // Descriptive message about the application status
    private String message;

    // Current status of the loan application
    private String status;

    // Timestamp of the response
    private LocalDateTime timestamp;

    // Optional: Approval probability (if applicable)
    private Double approvalProbability;

    // Default constructor
    public LoanApplicationResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructors with different parameter combinations
    public LoanApplicationResponse(String applicationId, String message, String status) {
        this.applicationId = applicationId;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public LoanApplicationResponse(
            String applicationId,
            String message,
            String status,
            Double approvalProbability
    ) {
        this.applicationId = applicationId;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.approvalProbability = approvalProbability;
    }

    // Builder pattern for flexible object creation
    public static class Builder {
        private String applicationId;
        private String message;
        private String status;
        private Double approvalProbability;

        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder approvalProbability(Double approvalProbability) {
            this.approvalProbability = approvalProbability;
            return this;
        }

        public LoanApplicationResponse build() {
            LoanApplicationResponse response = new LoanApplicationResponse(
                    this.applicationId,
                    this.message,
                    this.status
            );
            response.setApprovalProbability(this.approvalProbability);
            return response;
        }
    }

    // Static method to create a builder
    public static Builder builder() {
        return new Builder();
    }

    // Utility methods for common response scenarios
    public static LoanApplicationResponse success(String applicationId) {
        return new LoanApplicationResponse(
                applicationId,
                "Loan application submitted successfully",
                "SUBMITTED"
        );
    }

    public static LoanApplicationResponse error(String message) {
        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setMessage(message);
        response.setStatus("ERROR");
        return response;
    }

    // Getters and Setters
    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getApprovalProbability() {
        return approvalProbability;
    }

    public void setApprovalProbability(Double approvalProbability) {
        this.approvalProbability = approvalProbability;
    }

    // toString method for logging and debugging
    @Override
    public String toString() {
        return "LoanApplicationResponse{" +
                "applicationId='" + applicationId + '\'' +
                ", message='" + message + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", approvalProbability=" + approvalProbability +
                '}';
    }
}