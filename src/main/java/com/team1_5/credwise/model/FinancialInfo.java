package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "financial_info")
public class FinancialInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

//    @Column(name = "employer_name", nullable = false)
//    private String employerName;
//
//    @Column(nullable = false)
//    private String position;
//
//    @Column(name = "employment_type", nullable = false)
//    private String employmentType;
//
//    @Column(name = "employment_duration", nullable = false)
//    private Integer employmentDuration;

    @OneToMany(mappedBy = "financialInfo", cascade = CascadeType.ALL)
    private List<EmploymentHistory> employmentHistory;

    @Column(name = "monthly_income", nullable = false)
    private BigDecimal monthlyIncome;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Column(name = "monthly_expenses", nullable = false)
    private BigDecimal monthlyExpenses;

    @OneToMany(mappedBy = "financialInfo", cascade = CascadeType.ALL)
    private List<Debt> debts;

    @OneToMany(mappedBy = "financialInfo", cascade = CascadeType.ALL)
    private List<Asset> assets;

    public FinancialInfo() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LoanApplication getLoanApplication() { return loanApplication; }
    public void setLoanApplication(LoanApplication loanApplication) { this.loanApplication = loanApplication; }

//    public String getEmployerName() { return employerName; }
//    public void setEmployerName(String employerName) { this.employerName = employerName; }
//    public String getPosition() { return position; }
//    public void setPosition(String position) { this.position = position; }
//    public String getEmploymentType() { return employmentType; }
//    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
//    public Integer getEmploymentDuration() { return employmentDuration; }
//    public void setEmploymentDuration(Integer employmentDuration) { this.employmentDuration = employmentDuration; }
//    public BigDecimal getMonthlyIncome() { return monthlyIncome; }

    public List<EmploymentHistory> getEmploymentHistory() { return employmentHistory; }
    public void setEmploymentHistory(List<EmploymentHistory> employmentHistory) {this.employmentHistory = employmentHistory; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }
    public List<Debt> getDebts() { return debts; }
    public void setDebts(List<Debt> debts) { this.debts = debts; }
    public List<Asset> getAssets() { return assets; }
    public void setAssets(List<Asset> assets) { this.assets = assets; }



}

