package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
public class FinancialInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "financialInfo", cascade = CascadeType.ALL)
    private List<EmploymentHistory> employmentDetails;

    @OneToMany(mappedBy = "financialInfo", cascade = CascadeType.ALL)
    private List<Debt> existingDebts;

    @OneToMany(mappedBy = "financialInfo", cascade = CascadeType.ALL)
    private List<Asset> assets;

    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal estimatedDebts;
    private Integer creditScore;
    private BigDecimal currentCreditLimit;
    private BigDecimal creditTotalUsage;

    @OneToOne
    @JoinColumn(name = "loan_application_id")
    private LoanApplication loanApplication;

    // Getters and setters for FinancialInfo fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<EmploymentHistory> getEmploymentDetails() {
        return employmentDetails;
    }

    public void setEmploymentDetails(List<EmploymentHistory> employmentDetails) {
        this.employmentDetails = employmentDetails;
    }

    public List<Debt> getExistingDebts() {
        return existingDebts;
    }

    public void setExistingDebts(List<Debt> existingDebts) {
        this.existingDebts = existingDebts;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    public BigDecimal getEstimatedDebts() {
        return estimatedDebts;
    }

    public void setEstimatedDebts(BigDecimal estimatedDebts) {
        this.estimatedDebts = estimatedDebts;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    public BigDecimal getCurrentCreditLimit() {
        return currentCreditLimit;
    }

    public void setCurrentCreditLimit(BigDecimal currentCreditLimit) {
        this.currentCreditLimit = currentCreditLimit;
    }

    public BigDecimal getCreditTotalUsage() {
        return creditTotalUsage;
    }

    public void setCreditTotalUsage(BigDecimal creditTotalUsage) {
        this.creditTotalUsage = creditTotalUsage;
    }

    public LoanApplication getLoanApplication() {
        return loanApplication;
    }

    public void setLoanApplication(LoanApplication loanApplication) {
        this.loanApplication = loanApplication;
    }
}
