package com.team1_5.credwise.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FinancialSummaryResponse {
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private CreditScoreDetails creditScore;

    public static class CreditScoreDetails {
        private Integer score;
        private String range;
        private String lastUpdated;
        private Integer systemGeneratedCreditScore;

        // Constructor
        public CreditScoreDetails(Integer score, Integer systemGeneratedCreditScore, LocalDateTime lastUpdated) {
            this.score = score;
            this.systemGeneratedCreditScore = systemGeneratedCreditScore;
            // Format the date as a string
            this.lastUpdated = lastUpdated != null ? 
                lastUpdated.format(DateTimeFormatter.ISO_DATE_TIME) : null;
            
            // Set range based on score
            if (score == null) {
                this.range = "Unknown";
            } else if (score < 560) {
                this.range = "Poor";
            } else if (score < 660) {
                this.range = "Fair";
            } else if (score < 725) {
                this.range = "Good";
            } else if (score < 800) {
                this.range = "Very Good";
            } else {
                this.range = "Excellent";
            }
        }

        // Getters
        public Integer getScore() { return score; }
        public String getRange() { return range; }
        public String getLastUpdated() { return lastUpdated; }
        public Integer getSystemGeneratedCreditScore() { return systemGeneratedCreditScore; }
    }

    // Constructor
    public FinancialSummaryResponse(BigDecimal monthlyIncome, BigDecimal monthlyExpenses, 
                                   Integer score, Integer systemGeneratedCreditScore, 
                                   LocalDateTime lastUpdated) {
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.creditScore = new CreditScoreDetails(score, systemGeneratedCreditScore, lastUpdated);
    }

    // Getters
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public CreditScoreDetails getCreditScore() { return creditScore; }
}
