package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employment_history")
public class EmploymentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "financial_info_id", nullable = false)
    private FinancialInfo financialInfo;

    @Column(nullable = false)
    private String employerName;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private String employmentType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer durationMonths;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FinancialInfo getFinancialInfo() {
        return financialInfo;
    }

    public void setFinancialInfo(FinancialInfo financialInfo) {
        this.financialInfo = financialInfo;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }
}
