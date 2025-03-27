package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "financial_info_id", nullable = false)
    private FinancialInfo financialInfo;

    @Column(name = "asset_type", nullable = false)
    private String assetType;

    @Column
    private String description;

    @Column(name = "estimated_value", nullable = false)
    private BigDecimal estimatedValue;

    public Asset() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FinancialInfo getFinancialInfo() { return financialInfo; }
    public void setFinancialInfo(FinancialInfo financialInfo) { this.financialInfo = financialInfo; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }
}