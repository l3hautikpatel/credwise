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
}