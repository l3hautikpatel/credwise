package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal estimatedDebts;
    private Integer creditScore;
    @Column(name = "system_credit_score", nullable = true) // Allow null for system-generated score
    private Integer systemCreditScore;

    @Column(name = "eligibility_score")
    private Integer eligibilityScore;

    private BigDecimal currentCreditLimit;
    private BigDecimal creditTotalUsage;
    @Column(name = "credit_utilization")
    private BigDecimal creditUtilization;

    @OneToOne
    @JoinColumn(name = "loan_application_id")
    private LoanApplication loanApplication;

    @Column(name = "total_debts")
    private BigDecimal totalDebts;

    @Column(name = "debt_to_income_ratio")
    private BigDecimal debtToIncomeRatio;

    @Column(name = "total_assets")
    private BigDecimal totalAssets;

    @Column(name = "bank_accounts")
    private Integer bankAccounts;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;



    public FinancialInfo() {
        this.lastUpdated = LocalDateTime.now();
    }


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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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


    public Integer getSystemCreditScore() { return systemCreditScore; }
    public void setSystemCreditScore(Integer systemCreditScore) { this.systemCreditScore = systemCreditScore; }

    public Integer getEligibilityScore() { return eligibilityScore; }
    public void setEligibilityScore(Integer eligibilityScore) { this.eligibilityScore = eligibilityScore; }


    private void calculateCreditUtilization() {
        // Auto-calculate credit utilization
        if (currentCreditLimit.compareTo(BigDecimal.ZERO) != 0) {
            this.creditUtilization = creditTotalUsage
                    .divide(currentCreditLimit, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    public void setCreditUtilization(BigDecimal creditUtilization) {
        this.creditUtilization = creditUtilization;
    }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }


    public BigDecimal getTotalDebts() {
        return totalDebts;
    }

    public void setTotalDebts(BigDecimal totalDebts) {
        this.totalDebts = totalDebts;
    }

    public BigDecimal getDebtToIncomeRatio() {
        return debtToIncomeRatio;
    }

    public void setDebtToIncomeRatio(BigDecimal debtToIncomeRatio) {
        this.debtToIncomeRatio = debtToIncomeRatio;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public Integer getBankAccounts() {
        return bankAccounts;
    }

    public void setBankAccounts(Integer bankAccounts) {
        this.bankAccounts = bankAccounts;
    }

    public BigDecimal getCreditUtilization() {
        return creditUtilization;
    }


}

