package com.team1_5.credwise.dto;

import java.math.BigDecimal;
import java.util.List;

public class LoanApplicationResultResponse {
    private String status;
    private String message;
    private EligibilityDetails eligibilityDetails;
    private List<DecisionFactorResponse> decisionFactors;

    // Inner class for eligibility details
    public static class EligibilityDetails {
        private Integer eligibilityScore;
        private ApprovedTerms approvedTerms;

        public EligibilityDetails() {}

        public EligibilityDetails(Integer eligibilityScore, BigDecimal maxEligibleAmount,
                                 String suggestedInterestRate, Integer suggestedTerm, 
                                 BigDecimal estimatedMonthlyPayment) {
            this.eligibilityScore = eligibilityScore;
            this.approvedTerms = new ApprovedTerms(
                maxEligibleAmount, 
                suggestedInterestRate, 
                suggestedTerm, 
                estimatedMonthlyPayment
            );
        }

        public Integer getEligibilityScore() {
            return eligibilityScore;
        }

        public void setEligibilityScore(Integer eligibilityScore) {
            this.eligibilityScore = eligibilityScore;
        }

        public ApprovedTerms getApprovedTerms() {
            return approvedTerms;
        }

        public void setApprovedTerms(ApprovedTerms approvedTerms) {
            this.approvedTerms = approvedTerms;
        }
    }

    // Inner class for approved terms
    public static class ApprovedTerms {
        private BigDecimal maximumEligibleAmount;
        private String suggestedInterestRate;
        private Integer suggestedTerm;
        private BigDecimal estimatedMonthlyPayment;

        public ApprovedTerms() {}

        public ApprovedTerms(BigDecimal maximumEligibleAmount, String suggestedInterestRate,
                           Integer suggestedTerm, BigDecimal estimatedMonthlyPayment) {
            this.maximumEligibleAmount = maximumEligibleAmount;
            this.suggestedInterestRate = suggestedInterestRate;
            this.suggestedTerm = suggestedTerm;
            this.estimatedMonthlyPayment = estimatedMonthlyPayment;
        }

        public BigDecimal getMaximumEligibleAmount() {
            return maximumEligibleAmount;
        }

        public void setMaximumEligibleAmount(BigDecimal maximumEligibleAmount) {
            this.maximumEligibleAmount = maximumEligibleAmount;
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
    }

    // Inner class DecisionFactorResponse
    public static class DecisionFactorResponse {
        private String factor;
        private String impact;
        private String description;

        public DecisionFactorResponse() {}

        public DecisionFactorResponse(String factor, String impact, String description) {
            this.factor = factor;
            this.impact = impact;
            this.description = description;
        }

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

    // No-arg constructor
    public LoanApplicationResultResponse() {}

    // Constructor
    public LoanApplicationResultResponse(String status, String message, Integer eligibilityScore,
                                       BigDecimal maxEligibleAmount, String suggestedInterestRate,
                                       Integer suggestedTerm, BigDecimal estimatedMonthlyPayment,
                                       List<DecisionFactorResponse> decisionFactors) {
        this.status = status;
        this.message = message;
        this.eligibilityDetails = new EligibilityDetails(
            eligibilityScore, 
            maxEligibleAmount, 
            suggestedInterestRate, 
            suggestedTerm, 
            estimatedMonthlyPayment
        );
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

    public EligibilityDetails getEligibilityDetails() {
        return eligibilityDetails;
    }

    public void setEligibilityDetails(EligibilityDetails eligibilityDetails) {
        this.eligibilityDetails = eligibilityDetails;
    }

    public List<DecisionFactorResponse> getDecisionFactors() {
        return decisionFactors;
    }

    public void setDecisionFactors(List<DecisionFactorResponse> decisionFactors) {
        this.decisionFactors = decisionFactors;
    }
}
