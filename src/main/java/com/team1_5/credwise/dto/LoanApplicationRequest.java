package com.team1_5.credwise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LoanApplicationRequest {

    @Valid
    private LoanDetails loanDetails;

    @Valid
    private PersonalInfo personalInfo;

    @Valid
    private FinancialInfo financialInfo;

    @Valid
    private List<DocumentUpload> documents;

    // Getters and Setters for main class
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

    public List<DocumentUpload> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentUpload> documents) {
        this.documents = documents;
    }

    // Separate DocumentUpload class
    public static class DocumentUpload {
        @NotBlank(message = "Document type is required")
        @Size(max = 100, message = "Document type must be less than 100 characters")
        private String documentType;

        @NotBlank(message = "File content is required")
        private String file; // Base64 encoded string

        // Constructors
        public DocumentUpload() {}

        public DocumentUpload(String documentType, String file) {
            this.documentType = documentType;
            this.file = file;
        }

        // Getters and Setters
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

    // LoanDetails nested class
    public static class LoanDetails {
        @NotBlank(message = "Product type is required")
        @Size(max = 100, message = "Product type must be less than 100 characters")
        private String productType;

        @NotNull(message = "Requested amount is required")
        @Positive(message = "Requested amount must be positive")
        @DecimalMin(value = "1000.00", message = "Minimum loan amount is $1,000")
        @DecimalMax(value = "1000000.00", message = "Maximum loan amount is $1,000,000")
        private BigDecimal requestedAmount;

        @Size(max = 500, message = "Purpose description must be less than 500 characters")
        private String purposeDescription;

        @NotNull(message = "Requested term is required")
        @Positive(message = "Requested term must be positive")
        @Min(value = 1, message = "Minimum loan term is 1 month")
        @Max(value = 360, message = "Maximum loan term is 360 months (30 years)")
        private Integer requestedTerm;

        // Constructors
        public LoanDetails() {}

        public LoanDetails(String productType, BigDecimal requestedAmount,
                           String purposeDescription, Integer requestedTerm) {
            this.productType = productType;
            this.requestedAmount = requestedAmount;
            this.purposeDescription = purposeDescription;
            this.requestedTerm = requestedTerm;
        }

        // Getters and Setters
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

    // PersonalInfo nested class
    public static class PersonalInfo {
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must be less than 100 characters")
        private String email;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        private String phoneNumber;

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        private LocalDate dateOfBirth;

        @Valid
        @NotNull(message = "Address is required")
        private Address address;

        // Constructors
        public PersonalInfo() {}

        public PersonalInfo(String firstName, String lastName, String email,
                            String phoneNumber, LocalDate dateOfBirth, Address address) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.dateOfBirth = dateOfBirth;
            this.address = address;
        }

        // Getters and Setters
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

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        // Nested Address class
        public static class Address {
            @NotBlank(message = "Street is required")
            @Size(max = 200, message = "Street address must be less than 200 characters")
            private String street;

            @NotBlank(message = "City is required")
            @Size(max = 100, message = "City must be less than 100 characters")
            private String city;

            @NotBlank(message = "State is required")
            @Size(max = 100, message = "State must be less than 100 characters")
            private String state;

            @NotBlank(message = "Zip code is required")
            @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Invalid zip code format")
            private String zipCode;

            @NotBlank(message = "Country is required")
            @Size(max = 100, message = "Country must be less than 100 characters")
            private String country;

            @NotNull(message = "Address duration is required")
            @Positive(message = "Address duration must be positive")
            @Max(value = 100, message = "Address duration cannot exceed 100 years")
            private Integer duration;

            // Constructors
            public Address() {}

            public Address(String street, String city, String state,
                           String zipCode, String country, Integer duration) {
                this.street = street;
                this.city = city;
                this.state = state;
                this.zipCode = zipCode;
                this.country = country;
                this.duration = duration;
            }

            // Getters and Setters
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
    }

    // FinancialInfo nested class (similar structure, I'll omit for brevity to keep the response concise)
    public static class FinancialInfo {
        @Valid
        @NotNull(message = "Employment information is required")
        private Employment employment;

        @NotNull(message = "Monthly income is required")
        @Positive(message = "Monthly income must be positive")
        @DecimalMax(value = "1000000.00", message = "Monthly income is too high")
        private BigDecimal monthlyIncome;

        @NotNull(message = "Credit score is required")
        @Min(value = 300, message = "Minimum credit score is 300")
        @Max(value = 850, message = "Maximum credit score is 850")
        private Integer creditScore;

        @NotNull(message = "Monthly expenses are required")
        @PositiveOrZero(message = "Monthly expenses cannot be negative")
        @DecimalMax(value = "1000000.00", message = "Monthly expenses are too high")
        private BigDecimal monthlyExpenses;

        @Valid
        private List<Debt> debts;

        @Valid
        private List<Asset> assets;

        // Nested Employment class
        public static class Employment {
            @NotBlank(message = "Employer name is required")
            @Size(max = 200, message = "Employer name must be less than 200 characters")
            private String employerName;

            @NotBlank(message = "Position is required")
            @Size(max = 100, message = "Position must be less than 100 characters")
            private String position;

            @NotBlank(message = "Employment type is required")
            @Pattern(
                    regexp = "^(FULL_TIME|PART_TIME|CONTRACT|SELF_EMPLOYED|UNEMPLOYED)$",
                    message = "Invalid employment type"
            )
            private String employmentType;

            @NotNull(message = "Employment duration is required")
            @Positive(message = "Employment duration must be positive")
            @Max(value = 70, message = "Employment duration cannot exceed 70 years")
            private Integer employmentDuration;

            // Constructors
            public Employment() {}

            public Employment(String employerName, String position,
                              String employmentType, Integer employmentDuration) {
                this.employerName = employerName;
                this.position = position;
                this.employmentType = employmentType;
                this.employmentDuration = employmentDuration;
            }

            // Getters and Setters
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

        // Nested Debt class
        public static class Debt {
            @NotBlank(message = "Debt type is required")
            @Size(max = 100, message = "Debt type must be less than 100 characters")
            private String debtType;

            @NotBlank(message = "Lender is required")
            @Size(max = 200, message = "Lender name must be less than 200 characters")
            private String lender;

            @NotNull(message = "Outstanding amount is required")
            @PositiveOrZero(message = "Outstanding amount cannot be negative")
            @DecimalMax(value = "1000000.00", message = "Outstanding amount is too high")
            private BigDecimal outstandingAmount;

            @NotNull(message = "Monthly payment is required")
            @PositiveOrZero(message = "Monthly payment cannot be negative")
            @DecimalMax(value = "100000.00", message = "Monthly payment is too high")
            private BigDecimal monthlyPayment;

            @NotNull(message = "Interest rate is required")
            @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
            @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
            private BigDecimal interestRate;

            @NotNull(message = "Remaining term is required")
            @PositiveOrZero(message = "Remaining term cannot be negative")
            @Max(value = 360, message = "Remaining term cannot exceed 360 months")
            private Integer remainingTerm;

            // Constructors
            public Debt() {}

            public Debt(String debtType, String lender, BigDecimal outstandingAmount,
                        BigDecimal monthlyPayment, BigDecimal interestRate, Integer remainingTerm) {
                this.debtType = debtType;
                this.lender = lender;
                this.outstandingAmount = outstandingAmount;
                this.monthlyPayment = monthlyPayment;
                this.interestRate = interestRate;
                this.remainingTerm = remainingTerm;
            }

            // Getters and Setters (similar to previous implementations)
        }

        // Nested Asset class
        public static class Asset {
            @NotBlank(message = "Asset type is required")
            @Size(max = 100, message = "Asset type must be less than 100 characters")
            private String assetType;

            @Size(max = 500, message = "Asset description must be less than 500 characters")
            private String description;

            @NotNull(message = "Estimated value is required")
            @PositiveOrZero(message = "Estimated value cannot be negative")
            @DecimalMax(value = "10000000.00", message = "Asset value is too high")
            private BigDecimal estimatedValue;

            // Constructors
            public Asset() {}

            public Asset(String assetType, String description, BigDecimal estimatedValue) {
                this.assetType = assetType;
                this.description = description;
                this.estimatedValue = estimatedValue;
            }

            // Getters and Setters (similar to previous implementations)
        }

        // Constructors for FinancialInfo
        public FinancialInfo() {}

        public FinancialInfo(Employment employment, BigDecimal monthlyIncome,
                             Integer creditScore, BigDecimal monthlyExpenses,
                             List<Debt> debts, List<Asset> assets) {
            this.employment = employment;
            this.monthlyIncome = monthlyIncome;
            this.creditScore = creditScore;
            this.monthlyExpenses = monthlyExpenses;
            this.debts = debts;
            this.assets = assets;
        }

        // Getters and Setters
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
}