package com.team1_5.credwise.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FinancialSummaryResponse {
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private CreditScoreDetails creditScore;

    public static class CreditScoreDetails {
        private Integer score;
        private String range;
        private LocalDateTime lastUpdated;

        // Constructor
        public CreditScoreDetails(Integer score, String range, LocalDateTime lastUpdated) {
            this.score = score;
            this.range = range;
            this.lastUpdated = lastUpdated;
        }

        // Getters
        public Integer getScore() { return score; }
        public String getRange() { return range; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    // Constructor
    public FinancialSummaryResponse(BigDecimal monthlyIncome, BigDecimal monthlyExpenses, Integer score, String range, LocalDateTime lastUpdated) {
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.creditScore = new CreditScoreDetails(score, range, lastUpdated);
    }

    // Getters
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public CreditScoreDetails getCreditScore() { return creditScore; }
}
