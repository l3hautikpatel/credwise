package com.team1_5.credwise.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    @Id
    @Column(name = "application_id", unique = true, nullable = false)
    private String applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Embedded
    private LoanDetails loanDetails;

    @Embedded
    private PersonalInfo personalInfo;

    @Embedded
    private FinancialInfo financialInfo;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "application_id")
    private List<DocumentUpload> documents;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Column(name = "approval_probability")
    private Double approvalProbability;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "loan_application_required_documents", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "document_type")
    private List<String> requiredAdditionalDocuments;

    public LoanApplication() {
        this.applicationId = generateUniqueApplicationId();
        this.createdAt = LocalDateTime.now();
        this.status = ApplicationStatus.DRAFT;
    }

    private String generateUniqueApplicationId() {
        return UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LoanDetails getLoanDetails() { return loanDetails; }
    public void setLoanDetails(LoanDetails loanDetails) { this.loanDetails = loanDetails; }
    public PersonalInfo getPersonalInfo() { return personalInfo; }
    public void setPersonalInfo(PersonalInfo personalInfo) { this.personalInfo = personalInfo; }
    public FinancialInfo getFinancialInfo() { return financialInfo; }
    public void setFinancialInfo(FinancialInfo financialInfo) { this.financialInfo = financialInfo; }
    public List<DocumentUpload> getDocuments() { return documents; }
    public void setDocuments(List<DocumentUpload> documents) { this.documents = documents; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public Double getApprovalProbability() { return approvalProbability; }
    public void setApprovalProbability(Double approvalProbability) { this.approvalProbability = approvalProbability; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getRequiredAdditionalDocuments() { return requiredAdditionalDocuments; }
    public void setRequiredAdditionalDocuments(List<String> requiredAdditionalDocuments) { this.requiredAdditionalDocuments = requiredAdditionalDocuments; }

    @Embeddable
    public static class LoanDetails {
        private String productType;
        private BigDecimal requestedAmount;
        private String purposeDescription;
        private Integer requestedTerm;

        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        public BigDecimal getRequestedAmount() { return requestedAmount; }
        public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
        public String getPurposeDescription() { return purposeDescription; }
        public void setPurposeDescription(String purposeDescription) { this.purposeDescription = purposeDescription; }
        public Integer getRequestedTerm() { return requestedTerm; }
        public void setRequestedTerm(Integer requestedTerm) { this.requestedTerm = requestedTerm; }
    }

    @Embeddable
    public static class PersonalInfo {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private LocalDate dateOfBirth;
        @Embedded
        private Address address;

        // Getters and Setters
    }

    @Embeddable
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private Integer duration;

        // Getters and Setters
    }

    @Embeddable
    public static class FinancialInfo {
        private BigDecimal monthlyIncome;
        private BigDecimal monthlyExpenses;
        private Integer creditScore;
        @Embedded
        private Employment employment;
        @ElementCollection
        private List<Debt> debts;
        @ElementCollection
        private List<Asset> assets;

        // Getters and Setters
    }

    @Embeddable
    public static class Employment {
        private String employerName;
        private String position;
        private String employmentType;
        private Integer employmentDuration;

        // Getters and Setters
    }

    @Embeddable
    public static class Debt {
        private String debtType;
        private String lender;
        private BigDecimal outstandingAmount;
        private BigDecimal monthlyPayment;
        private BigDecimal interestRate;
        private Integer remainingTerm;

        // Getters and Setters
    }

    @Embeddable
    public static class Asset {
        private String assetType;
        private String description;
        private BigDecimal estimatedValue;

        // Getters and Setters
    }

    @Entity
    @Table(name = "loan_application_documents")
    public static class DocumentUpload {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String documentType;
        @Lob
        private byte[] fileContent;
        private String fileName;
        private String fileType;

        // Getters and Setters
    }

    public enum ApplicationStatus {
        DRAFT, SUBMITTED, UNDER_REVIEW, REQUIRES_MORE_INFO, APPROVED, REJECTED, CANCELLED
    }
}
