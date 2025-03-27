package com.team1_5.credwise.dto;

import java.math.BigDecimal;
import java.util.List;

public class LoanApplicationResultResponse {
    private String status;
    private String message;
    private Integer eligibilityScore;
    private BigDecimal maxEligibleAmount;
    private String suggestedInterestRate;
    private Integer suggestedTerm;
    private BigDecimal estimatedMonthlyPayment;
    private List<DecisionFactorResponse> decisionFactors;

    // Constructor
    // No-arg constructor
    public LoanApplicationResultResponse() {}

    public LoanApplicationResultResponse(String status, String message, Integer eligibilityScore,
                                         BigDecimal maxEligibleAmount, String suggestedInterestRate,
                                         Integer suggestedTerm, BigDecimal estimatedMonthlyPayment,
                                         List<DecisionFactorResponse> decisionFactors) {
        this.status = status;
        this.message = message;
        this.eligibilityScore = eligibilityScore;
        this.maxEligibleAmount = maxEligibleAmount;
        this.suggestedInterestRate = suggestedInterestRate;
        this.suggestedTerm = suggestedTerm;
        this.estimatedMonthlyPayment = estimatedMonthlyPayment;
        this.decisionFactors = decisionFactors;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getEligibilityScore() {
        return eligibilityScore;
    }

    public void setEligibilityScore(Integer eligibilityScore) {
        this.eligibilityScore = eligibilityScore;
    }

    public BigDecimal getMaxEligibleAmount() {
        return maxEligibleAmount;
    }

    public void setMaxEligibleAmount(BigDecimal maxEligibleAmount) {
        this.maxEligibleAmount = maxEligibleAmount;
    }

    public String getSuggestedInterestRate() {
        return suggestedInterestRate;
    }

    public void setSuggestedInterestRate(String suggestedInterestRate) {
        this.suggestedInterestRate = suggestedInterestRate;
    }

    public Integer getSuggestedTerm() {
        return suggestedTerm;
    }

    public void setSuggestedTerm(Integer suggestedTerm) {
        this.suggestedTerm = suggestedTerm;
    }

    public BigDecimal getEstimatedMonthlyPayment() {
        return estimatedMonthlyPayment;
    }

    public void setEstimatedMonthlyPayment(BigDecimal estimatedMonthlyPayment) {
        this.estimatedMonthlyPayment = estimatedMonthlyPayment;
    }

    public List<DecisionFactorResponse> getDecisionFactors() {
        return decisionFactors;
    }

    public void setDecisionFactors(List<DecisionFactorResponse> decisionFactors) {
        this.decisionFactors = decisionFactors;
    }

    // Inner class DecisionFactorResponse
    public static class DecisionFactorResponse {
        private String factor;
        private String impact;
        private String description;


        public  DecisionFactorResponse() {} ;

        // Constructor
        public DecisionFactorResponse(String factor, String impact, String description) {
            this.factor = factor;
            this.impact = impact;
            this.description = description;
        }

        // Getters and Setters
        public String getFactor() {
            return factor;
        }

        public void setFactor(String factor) {
            this.factor = factor;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String impact) {
            this.impact = impact;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
