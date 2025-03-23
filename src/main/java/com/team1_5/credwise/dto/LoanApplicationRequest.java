package com.team1_5.credwise.dto;

import java.math.BigDecimal;
import java.util.List;

public class LoanApplicationRequest {
    private LoanDetails loanDetails;
    private PersonalInfo personalInfo;
    private FinancialInfo financialInfo;
    private List<DocumentRequest> documents;

    // Getters and Setters
    public LoanDetails getLoanDetails() {
        return loanDetails;
    }

    public void setLoanDetails(LoanDetails loanDetails) {
        this.loanDetails = loanDetails;
    }

    public PersonalInfo getPersonalInfo() {
        return personalInfo;
    }

    public void setPersonalInfo(PersonalInfo personalInfo) {
        this.personalInfo = personalInfo;
    }

    public FinancialInfo getFinancialInfo() {
        return financialInfo;
    }

    public void setFinancialInfo(FinancialInfo financialInfo) {
        this.financialInfo = financialInfo;
    }

    public List<DocumentRequest> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentRequest> documents) {
        this.documents = documents;
    }

    // Nested static classes with Getters/Setters
    public static class LoanDetails {
        private String productType;
        private BigDecimal requestedAmount;
        private String purposeDescription;
        private Integer requestedTerm;

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

        public Integer getRequestedTerm() {
            return requestedTerm;
        }

        public void setRequestedTerm(Integer requestedTerm) {
            this.requestedTerm = requestedTerm;
        }
    }

    public static class PersonalInfo {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String dateOfBirth;
        private Address address;

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private Integer duration;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }
    }

    public static class FinancialInfo {
        private Employment employment;
        private BigDecimal monthlyIncome;
        private Integer creditScore;
        private BigDecimal monthlyExpenses;
        private List<Debt> debts;
        private List<Asset> assets;

        public Employment getEmployment() {
            return employment;
        }

        public void setEmployment(Employment employment) {
            this.employment = employment;
        }

        public BigDecimal getMonthlyIncome() {
            return monthlyIncome;
        }

        public void setMonthlyIncome(BigDecimal monthlyIncome) {
            this.monthlyIncome = monthlyIncome;
        }

        public Integer getCreditScore() {
            return creditScore;
        }

        public void setCreditScore(Integer creditScore) {
            this.creditScore = creditScore;
        }

        public BigDecimal getMonthlyExpenses() {
            return monthlyExpenses;
        }

        public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
            this.monthlyExpenses = monthlyExpenses;
        }

        public List<Debt> getDebts() {
            return debts;
        }

        public void setDebts(List<Debt> debts) {
            this.debts = debts;
        }

        public List<Asset> getAssets() {
            return assets;
        }

        public void setAssets(List<Asset> assets) {
            this.assets = assets;
        }
    }

    public static class Employment {
        private String employerName;
        private String position;
        private String employmentType;
        private Integer employmentDuration;

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

        public Integer getEmploymentDuration() {
            return employmentDuration;
        }

        public void setEmploymentDuration(Integer employmentDuration) {
            this.employmentDuration = employmentDuration;
        }
    }

    public static class Debt {
        private String debtType;
        private String lender;
        private BigDecimal outstandingAmount;
        private BigDecimal monthlyPayment;
        private BigDecimal interestRate;
        private Integer remainingTerm;

        public String getDebtType() {
            return debtType;
        }

        public void setDebtType(String debtType) {
            this.debtType = debtType;
        }

        public String getLender() {
            return lender;
        }

        public void setLender(String lender) {
            this.lender = lender;
        }

        public BigDecimal getOutstandingAmount() {
            return outstandingAmount;
        }

        public void setOutstandingAmount(BigDecimal outstandingAmount) {
            this.outstandingAmount = outstandingAmount;
        }

        public BigDecimal getMonthlyPayment() {
            return monthlyPayment;
        }

        public void setMonthlyPayment(BigDecimal monthlyPayment) {
            this.monthlyPayment = monthlyPayment;
        }

        public BigDecimal getInterestRate() {
            return interestRate;
        }

        public void setInterestRate(BigDecimal interestRate) {
            this.interestRate = interestRate;
        }

        public Integer getRemainingTerm() {
            return remainingTerm;
        }

        public void setRemainingTerm(Integer remainingTerm) {
            this.remainingTerm = remainingTerm;
        }
    }

    public static class Asset {
        private String assetType;
        private String description;
        private BigDecimal estimatedValue;

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

    public static class DocumentRequest {
        private String documentType;
        private String file;

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
