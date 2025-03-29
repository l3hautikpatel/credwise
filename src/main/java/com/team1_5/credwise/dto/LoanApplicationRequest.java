// LoanApplicationRequest.java

package com.team1_5.credwise.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LoanApplicationRequest {
    private LoanDetails loanDetails;
    private PersonalInformation personalInformation;
    private FinancialInformation financialInformation;
    private List<DocumentRequest> documents;

    // Getters and setters for LoanApplicationRequest
    public LoanDetails getLoanDetails() {
        return loanDetails;
    }

    public void setLoanDetails(LoanDetails loanDetails) {
        this.loanDetails = loanDetails;
    }

    public PersonalInformation getPersonalInformation() {
        return personalInformation;
    }

    public void setPersonalInformation(PersonalInformation personalInformation) {
        this.personalInformation = personalInformation;
    }

    public FinancialInformation getFinancialInformation() {
        return financialInformation;
    }

    public void setFinancialInformation(FinancialInformation financialInformation) {
        this.financialInformation = financialInformation;
    }

    public List<DocumentRequest> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentRequest> documents) {
        this.documents = documents;
    }

    // Nested classes with validation
    public static class LoanDetails {
        @NotBlank private String productType;
        @Positive private BigDecimal requestedAmount;
        private String purposeDescription;
        @NotNull(message = "Requested term months is required")
        @Positive(message = "Requested term must be positive")
        private Integer requestedTermMonths;

        // Constructor
        public LoanDetails(String productType, BigDecimal requestedAmount, String purposeDescription, Integer requestedTermMonths) {
            this.productType = productType;
            this.requestedAmount = requestedAmount;
            this.purposeDescription = purposeDescription;
            this.requestedTermMonths = requestedTermMonths;
        }

        // Getters and setters
        public String getProductType() {
            return productType;
        }

        public void setProductType(String productType) {
            this.productType = productType;
        }

        public BigDecimal getRequestedAmount() {
            return requestedAmount;
        }

        public void setRequestedAmount(BigDecimal requestedAmount) {
            this.requestedAmount = requestedAmount;
        }

        public String getPurposeDescription() {
            return purposeDescription;
        }

        public void setPurposeDescription(String purposeDescription) {
            this.purposeDescription = purposeDescription;
        }

        public Integer getRequestedTermMonths() {
            return requestedTermMonths;
        }

        public void setRequestedTermMonths(Integer requestedTermMonths) {
            this.requestedTermMonths = requestedTermMonths;
        }
    }

    public static class PersonalInformation {
        @NotBlank
        private String firstName;
        @NotBlank private String lastName;
        @Email
        private String emailAddress;
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") private String phoneNumber;
        @Past private LocalDate dateOfBirth;
        private CurrentAddress currentAddress;

        // Constructor
        public PersonalInformation(String firstName, String lastName, String emailAddress, String phoneNumber, LocalDate dateOfBirth, CurrentAddress currentAddress) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.emailAddress = emailAddress;
            this.phoneNumber = phoneNumber;
            this.dateOfBirth = dateOfBirth;
            this.currentAddress = currentAddress;
        }

        // Getters and setters
        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public CurrentAddress getCurrentAddress() {
            return currentAddress;
        }

        public void setCurrentAddress(CurrentAddress currentAddress) {
            this.currentAddress = currentAddress;
        }

        public static class CurrentAddress {
            @NotBlank private String streetAddress;
            @NotBlank private String city;
            @NotBlank private String province;
            @NotBlank @Size(min = 6, max = 6) private String postalCode;
            @NotBlank private String country;
            @Min(0) private Integer durationAtAddressMonths;

            // Constructor
            public CurrentAddress(String streetAddress, String city, String province, String postalCode, String country, Integer durationAtAddressMonths) {
                this.streetAddress = streetAddress;
                this.city = city;
                this.province = province;
                this.postalCode = postalCode;
                this.country = country;
                this.durationAtAddressMonths = durationAtAddressMonths;
            }

            // Getters and setters
            public String getStreetAddress() {
                return streetAddress;
            }

            public void setStreetAddress(String streetAddress) {
                this.streetAddress = streetAddress;
            }

            public String getCity() {
                return city;
            }

            public void setCity(String city) {
                this.city = city;
            }

            public String getProvince() {
                return province;
            }

            public void setProvince(String province) {
                this.province = province;
            }

            public String getPostalCode() {
                return postalCode;
            }

            public void setPostalCode(String postalCode) {
                this.postalCode = postalCode;
            }

            public String getCountry() {
                return country;
            }

            public void setCountry(String country) {
                this.country = country;
            }

            public Integer getDurationAtAddressMonths() {
                return durationAtAddressMonths;
            }

            public void setDurationAtAddressMonths(Integer durationAtAddressMonths) {
                this.durationAtAddressMonths = durationAtAddressMonths;
            }
        }
    }

    public static class FinancialInformation {
        @NotEmpty private List<EmploymentDetail> employmentDetails;
        @Positive private BigDecimal monthlyIncome;
        @Positive private BigDecimal monthlyExpenses;
        @PositiveOrZero private BigDecimal estimatedDebts;
        @Min(300) @Max(850) private Integer creditScore;
        @PositiveOrZero private BigDecimal currentCreditLimit;
        @PositiveOrZero private BigDecimal creditTotalUsage;
        private List<ExistingDebt> existingDebts;
        private List<Asset> assets;

        // Constructor
        public FinancialInformation(List<EmploymentDetail> employmentDetails, BigDecimal monthlyIncome, BigDecimal monthlyExpenses, BigDecimal estimatedDebts, Integer creditScore, BigDecimal currentCreditLimit, BigDecimal creditTotalUsage, List<ExistingDebt> existingDebts, List<Asset> assets) {
            this.employmentDetails = employmentDetails;
            this.monthlyIncome = monthlyIncome;
            this.monthlyExpenses = monthlyExpenses;
            this.estimatedDebts = estimatedDebts;
            this.creditScore = creditScore;
            this.currentCreditLimit = currentCreditLimit;
            this.creditTotalUsage = creditTotalUsage;
            this.existingDebts = existingDebts;
            this.assets = assets;
        }

        // Getters and setters
        public List<EmploymentDetail> getEmploymentDetails() {
            return employmentDetails;
        }

        public void setEmploymentDetails(List<EmploymentDetail> employmentDetails) {
            this.employmentDetails = employmentDetails;
        }

        public BigDecimal getMonthlyIncome() {
            return monthlyIncome;
        }

        public void setMonthlyIncome(BigDecimal monthlyIncome) {
            this.monthlyIncome = monthlyIncome;
        }

        public BigDecimal getMonthlyExpenses() {
            return monthlyExpenses;
        }

        public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
            this.monthlyExpenses = monthlyExpenses;
        }

        public BigDecimal getEstimatedDebts() {
            return estimatedDebts;
        }

        public void setEstimatedDebts(BigDecimal estimatedDebts) {
            this.estimatedDebts = estimatedDebts;
        }

        public Integer getCreditScore() {
            return creditScore;
        }

        public void setCreditScore(Integer creditScore) {
            this.creditScore = creditScore;
        }

        public BigDecimal getCurrentCreditLimit() {
            return currentCreditLimit;
        }

        public void setCurrentCreditLimit(BigDecimal currentCreditLimit) {
            this.currentCreditLimit = currentCreditLimit;
        }

        public BigDecimal getCreditTotalUsage() {
            return creditTotalUsage;
        }

        public void setCreditTotalUsage(BigDecimal creditTotalUsage) {
            this.creditTotalUsage = creditTotalUsage;
        }

        public List<ExistingDebt> getExistingDebts() {
            return existingDebts;
        }

        public void setExistingDebts(List<ExistingDebt> existingDebts) {
            this.existingDebts = existingDebts;
        }

        public List<Asset> getAssets() {
            return assets;
        }

        public void setAssets(List<Asset> assets) {
            this.assets = assets;
        }

        public static class EmploymentDetail {
            @NotBlank private String employerName;
            @NotBlank private String position;
            private LocalDate startDate;
            private LocalDate endDate;
            @NotBlank private String employmentType;
            @Positive private Integer employmentDurationMonths;

            // Constructor
            public EmploymentDetail(String employerName, String position, LocalDate startDate, LocalDate endDate, String employmentType, Integer employmentDurationMonths) {
                this.employerName = employerName;
                this.position = position;
                this.startDate = startDate;
                this.endDate = endDate;
                this.employmentType = employmentType;
                this.employmentDurationMonths = employmentDurationMonths;
            }

            // Getters and setters
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

            public String getEmploymentType() {
                return employmentType;
            }

            public void setEmploymentType(String employmentType) {
                this.employmentType = employmentType;
            }

            public Integer getEmploymentDurationMonths() {
                return employmentDurationMonths;
            }

            public void setEmploymentDurationMonths(Integer employmentDurationMonths) {
                this.employmentDurationMonths = employmentDurationMonths;
            }
        }

        public static class ExistingDebt {
            @NotBlank private String debtType;
            @Positive private BigDecimal outstandingAmount;
            @DecimalMin("0.0") private BigDecimal interestRate;
            @Positive private BigDecimal monthlyPayment;
            @Positive private Integer remainingTermMonths;
            private String lender;
            private String paymentHistory;

            // Constructor
            public ExistingDebt(String debtType, BigDecimal outstandingAmount, BigDecimal interestRate, BigDecimal monthlyPayment, Integer remainingTermMonths, String lender, String paymentHistory) {
                this.debtType = debtType;
                this.outstandingAmount = outstandingAmount;
                this.interestRate = interestRate;
                this.monthlyPayment = monthlyPayment;
                this.remainingTermMonths = remainingTermMonths;
                this.lender = lender;
                this.paymentHistory = paymentHistory;
            }

            // Getters and setters
            public String getDebtType() {
                return debtType;
            }

            public void setDebtType(String debtType) {
                this.debtType = debtType;
            }

            public BigDecimal getOutstandingAmount() {
                return outstandingAmount;
            }

            public void setOutstandingAmount(BigDecimal outstandingAmount) {
                this.outstandingAmount = outstandingAmount;
            }

            public BigDecimal getInterestRate() {
                return interestRate;
            }

            public void setInterestRate(BigDecimal interestRate) {
                this.interestRate = interestRate;
            }

            public BigDecimal getMonthlyPayment() {
                return monthlyPayment;
            }

            public void setMonthlyPayment(BigDecimal monthlyPayment) {
                this.monthlyPayment = monthlyPayment;
            }

            public Integer getRemainingTermMonths() {
                return remainingTermMonths;
            }

            public void setRemainingTermMonths(Integer remainingTermMonths) {
                this.remainingTermMonths = remainingTermMonths;
            }

            public String getLender() {
                return lender;
            }

            public void setLender(String lender) {
                this.lender = lender;
            }

            public String getPaymentHistory() {
                return paymentHistory;
            }

            public void setPaymentHistory(String paymentHistory) {
                this.paymentHistory = paymentHistory;
            }
        }

        public static class Asset {
            @NotBlank private String assetType;
            private String description;
            @Positive private BigDecimal estimatedValue;

            // Constructor
            public Asset(String assetType, String description, BigDecimal estimatedValue) {
                this.assetType = assetType;
                this.description = description;
                this.estimatedValue = estimatedValue;
            }

            // Getters and setters
            public String getAssetType() {
                return assetType;
            }

            public void setAssetType(String assetType) {
                this.assetType = assetType;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public BigDecimal getEstimatedValue() {
                return estimatedValue;
            }

            public void setEstimatedValue(BigDecimal estimatedValue) {
                this.estimatedValue = estimatedValue;
            }
        }
    }

    public static class DocumentRequest {
        @NotBlank private String documentType;
        @NotBlank private String file; // Base64 string

        // Constructor
        public DocumentRequest(String documentType, String file) {
            this.documentType = documentType;
            this.file = file;
        }

        // Getters and setters
        public String getDocumentType() {
            return documentType;
        }

        public void setDocumentType(String documentType) {
            this.documentType = documentType;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }
}
