package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_summary")
public class FinancialSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "monthly_income", nullable = false)
    private BigDecimal monthlyIncome;

    @Column(name = "monthly_expenses", nullable = false)
    private BigDecimal monthlyExpenses;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

//    @Column(name = "score_range", nullable = false)
//    private String scoreRange;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;


    @Column(name = "estimated_debts", nullable = false)
    private BigDecimal estimatedDebts;

    @Column(name = "current_credit_limit", nullable = false)
    private BigDecimal currentCreditLimit;

    @Column(name = "credit_total_usage", nullable = false)
    private BigDecimal creditTotalUsage;

    @Column(name = "credit_utilization", nullable = false)
    private BigDecimal creditUtilization;

    @Column(name = "system_credit_score", nullable = true) // Allow null for system-generated score
    private Integer systemCreditScore;

    @Column(name = "debt_to_income_ratio")
    private BigDecimal debtToIncomeRatio;

    @Column(name = "total_assets")
    private BigDecimal totalAssets;


    public FinancialSummary() {
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }
    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
//    public String getScoreRange() { return scoreRange; }
//    public void setScoreRange(String scoreRange) { this.scoreRange = scoreRange; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    // New getters/setters
    public BigDecimal getEstimatedDebts() { return estimatedDebts; }
    public void setEstimatedDebts(BigDecimal estimatedDebts) { this.estimatedDebts = estimatedDebts; }

    public BigDecimal getCurrentCreditLimit() { return currentCreditLimit; }
    public void setCurrentCreditLimit(BigDecimal currentCreditLimit) {
        this.currentCreditLimit = currentCreditLimit;
        calculateCreditUtilization();
    }

    public BigDecimal getCreditTotalUsage() { return creditTotalUsage; }
    public void setCreditTotalUsage(BigDecimal creditTotalUsage) {
        this.creditTotalUsage = creditTotalUsage;
        calculateCreditUtilization();
    }

    private void calculateCreditUtilization() {
        if(currentCreditLimit.compareTo(BigDecimal.ZERO) > 0) {
            this.creditUtilization = creditTotalUsage
                    .divide(currentCreditLimit, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    public Integer getSystemCreditScore() { return systemCreditScore; }
    public void setSystemCreditScore(Integer systemCreditScore) { this.systemCreditScore = systemCreditScore; }

}