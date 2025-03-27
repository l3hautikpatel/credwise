package com.team1_5.credwise.model;

import jakarta.persistence.*;

@Entity
@Table(name = "decision_factors")
public class DecisionFactor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "result_id", nullable = false)
    private LoanApplicationResult result;

    @Column(nullable = false)
    private String factor;

    @Column(nullable = false)
    private String impact;

    @Column(nullable = false)
    private String description;

    public DecisionFactor() {}

    // Getters
    public Long getId() { return id; }
    public LoanApplicationResult getResult() { return result; }
    public String getFactor() { return factor; }
    public String getImpact() { return impact; }
    public String getDescription() { return description; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setResult(LoanApplicationResult result) { this.result = result; }
    public void setFactor(String factor) { this.factor = factor; }
    public void setImpact(String impact) { this.impact = impact; }
    public void setDescription(String description) { this.description = description; }
}