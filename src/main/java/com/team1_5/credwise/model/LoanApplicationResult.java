package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "loan_application_results")
public class LoanApplicationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String message;

    @Column(name = "eligibility_score", nullable = false)
    private Integer eligibilityScore;

    @Column(name = "max_eligible_amount", nullable = false)
    private BigDecimal maxEligibleAmount;

    @Column(name = "suggested_interest_rate", nullable = false)
    private String suggestedInterestRate;

    @Column(name = "suggested_term", nullable = false)
    private Integer suggestedTerm;

    @Column(name = "estimated_monthly_payment", nullable = false)
    private BigDecimal estimatedMonthlyPayment;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL)
    private List<DecisionFactor> decisionFactors;

    public LoanApplicationResult() {}

    // Getters
    public Long getId() { return id; }
    public LoanApplication getLoanApplication() { return loanApplication; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Integer getEligibilityScore() { return eligibilityScore; }
    public BigDecimal getMaxEligibleAmount() { return maxEligibleAmount; }
    public String getSuggestedInterestRate() { return suggestedInterestRate; }
    public Integer getSuggestedTerm() { return suggestedTerm; }
    public BigDecimal getEstimatedMonthlyPayment() { return estimatedMonthlyPayment; }
    public List<DecisionFactor> getDecisionFactors() { return decisionFactors; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setLoanApplication(LoanApplication loanApplication) { this.loanApplication = loanApplication; }
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setEligibilityScore(Integer eligibilityScore) { this.eligibilityScore = eligibilityScore; }
    public void setMaxEligibleAmount(BigDecimal maxEligibleAmount) { this.maxEligibleAmount = maxEligibleAmount; }
    public void setSuggestedInterestRate(String suggestedInterestRate) { this.suggestedInterestRate = suggestedInterestRate; }
    public void setSuggestedTerm(Integer suggestedTerm) { this.suggestedTerm = suggestedTerm; }
    public void setEstimatedMonthlyPayment(BigDecimal estimatedMonthlyPayment) { this.estimatedMonthlyPayment = estimatedMonthlyPayment; }
    public void setDecisionFactors(List<DecisionFactor> decisionFactors) { this.decisionFactors = decisionFactors; }
}