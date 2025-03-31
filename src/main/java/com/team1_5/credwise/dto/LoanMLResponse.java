package com.team1_5.credwise.dto;

import java.util.Map;

public class LoanMLResponse {
    private Long applicationId;
    private String status;
    private Integer systemCreditScore;
    private Integer eligibilityScore;
    private Map<String, Object> decision;

    public LoanMLResponse(Long applicationId, String status, Integer systemCreditScore, Integer eligibilityScore, Map<String, Object> decision) {
        this.applicationId = applicationId;
        this.status = status;
        this.systemCreditScore = systemCreditScore;
        this.eligibilityScore = eligibilityScore;
        this.decision = decision;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSystemCreditScore() {
        return systemCreditScore;
    }

    public void setSystemCreditScore(Integer systemCreditScore) {
        this.systemCreditScore = systemCreditScore;
    }

    public Integer getEligibilityScore() {
        return eligibilityScore;
    }

    public void setEligibilityScore(Integer eligibilityScore) {
        this.eligibilityScore = eligibilityScore;
    }

    public Map<String, Object> getDecision() {
        return decision;
    }

    public void setDecision(Map<String, Object> decision) {
        this.decision = decision;
    }
} 