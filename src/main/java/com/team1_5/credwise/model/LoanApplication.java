package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String status;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "requested_amount", nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "purpose_description")
    private String purposeDescription;

    @Column(name = "requested_term", nullable = false)
    private Integer requestedTerm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public LoanApplication() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    public String getPurposeDescription() { return purposeDescription; }
    public void setPurposeDescription(String purposeDescription) { this.purposeDescription = purposeDescription; }
    public Integer getRequestedTerm() { return requestedTerm; }
    public void setRequestedTerm(Integer requestedTerm) { this.requestedTerm = requestedTerm; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}