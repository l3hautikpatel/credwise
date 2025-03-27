package com.team1_5.credwise.model;

import jakarta.persistence.*;

@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Lob
    @Column(name = "file_data", nullable = false)
    private String fileData;

    public Document() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LoanApplication getLoanApplication() { return loanApplication; }
    public void setLoanApplication(LoanApplication loanApplication) { this.loanApplication = loanApplication; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
}