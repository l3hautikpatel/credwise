package com.team1_5.credwise.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Entity
@Table(name = "debts")
public class Debt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "financial_info_id", nullable = false)
    private FinancialInfo financialInfo;

    @Column(name = "debt_type", nullable = false)
    @NotBlank(message = "Debt type is required")
    private String debtType;

    @Column(nullable = false)
    private String lender;

    @Column(name = "outstanding_amount", nullable = false)
    private BigDecimal outstandingAmount;

    @Column(name = "monthly_payment", nullable = false)
    private BigDecimal monthlyPayment;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "remaining_term", nullable = false)
    private Integer remainingTerm;

    @Column(name = "remaining_term_months")
    private Integer remainingTermMonths;

    @Column(name = "payment_history")
    private String paymentHistory;

    public Debt() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FinancialInfo getFinancialInfo() { return financialInfo; }
    public void setFinancialInfo(FinancialInfo financialInfo) { this.financialInfo = financialInfo; }
    public String getDebtType() { return debtType; }
    public void setDebtType(String debtType) { this.debtType = debtType; }
    public String getLender() { return lender; }
    public void setLender(String lender) { this.lender = lender; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getRemainingTerm() { return remainingTerm; }
    public void setRemainingTerm(Integer remainingTerm) { this.remainingTerm = remainingTerm; }

    public void setRemainingTermMonths(Integer remainingTermMonths) {
        this.remainingTermMonths = remainingTermMonths;
    }

    public void setPaymentHistory(String paymentHistory) {
        this.paymentHistory = paymentHistory;
    }
    public String getPaymentHistory() { return paymentHistory; }

    public Integer getRemainingTermMonths() {
        return remainingTermMonths;
    }
}