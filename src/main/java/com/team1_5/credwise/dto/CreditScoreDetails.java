package com.team1_5.credwise.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Credit Score Details
 *
 * Represents comprehensive credit-related information for loan applications
 */
public class CreditScoreDetails {
    private int creditScore;
    private BigDecimal annualIncome;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal selfReportedDebt;
    private int creditUtilization;
    private int numOpenAccounts;
    private int numCreditInquiries;
    private String paymentHistory;
    private BigDecimal currentCreditLimit;

    // Constructors
    public CreditScoreDetails() {}

    /**
     * Comprehensive constructor to initialize all attributes
     */
    public CreditScoreDetails(
            int creditScore,
            BigDecimal annualIncome,
            BigDecimal monthlyIncome,
            BigDecimal monthlyExpenses,
            BigDecimal selfReportedDebt,
            int creditUtilization,
            int numOpenAccounts,
            int numCreditInquiries,
            String paymentHistory,
            BigDecimal currentCreditLimit
    ) {
        this.creditScore = creditScore;
        this.annualIncome = annualIncome;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.selfReportedDebt = selfReportedDebt;
        this.creditUtilization = creditUtilization;
        this.numOpenAccounts = numOpenAccounts;
        this.numCreditInquiries = numCreditInquiries;
        this.paymentHistory = paymentHistory;
        this.currentCreditLimit = currentCreditLimit;
    }

    // Getters and Setters
    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
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

    public BigDecimal getSelfReportedDebt() {
        return selfReportedDebt;
    }

    public void setSelfReportedDebt(BigDecimal selfReportedDebt) {
        this.selfReportedDebt = selfReportedDebt;
    }

    public int getCreditUtilization() {
        return creditUtilization;
    }

    public void setCreditUtilization(int creditUtilization) {
        this.creditUtilization = creditUtilization;
    }

    public int getNumOpenAccounts() {
        return numOpenAccounts;
    }

    public void setNumOpenAccounts(int numOpenAccounts) {
        this.numOpenAccounts = numOpenAccounts;
    }

    public int getNumCreditInquiries() {
        return numCreditInquiries;
    }

    public void setNumCreditInquiries(int numCreditInquiries) {
        this.numCreditInquiries = numCreditInquiries;
    }

    public String getPaymentHistory() {
        return paymentHistory;
    }

    public void setPaymentHistory(String paymentHistory) {
        this.paymentHistory = paymentHistory;
    }

    public BigDecimal getCurrentCreditLimit() {
        return currentCreditLimit;
    }

    public void setCurrentCreditLimit(BigDecimal currentCreditLimit) {
        this.currentCreditLimit = currentCreditLimit;
    }
}
