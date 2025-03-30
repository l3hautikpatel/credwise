package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.exception.LoanApplicationException;
import com.team1_5.credwise.model.*;
import com.team1_5.credwise.repository.*;
import com.team1_5.credwise.util.CreditScoreService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoanApplicationService {

    private final LoanApplicationRepository loanAppRepo;
    private final PersonalInfoRepository personalInfoRepo;
    private final AddressRepository addressRepo;
    private final FinancialInfoRepository financialInfoRepo;
    private final EmploymentHistoryRepository employmentRepo;
    private final DebtRepository debtRepo;
    private final AssetRepository assetRepo;
    private final DocumentRepository documentRepo;
    private final CreditScoreService creditScoreService;
    private final UserRepository userRepo;
    private final LoanMLService loanMLService;
    private final LoanApplicationResultService loanApplicationResultService;

    public LoanApplicationService(LoanApplicationRepository loanAppRepo,
                                  PersonalInfoRepository personalInfoRepo,
                                  AddressRepository addressRepo,
                                  FinancialInfoRepository financialInfoRepo,
                                  EmploymentHistoryRepository employmentRepo,
                                  DebtRepository debtRepo,
                                  AssetRepository assetRepo,
                                  DocumentRepository documentRepo,
                                  CreditScoreService creditScoreService,
                                  UserRepository userRepo,
                                  LoanMLService loanMLService,
                                  LoanApplicationResultService loanApplicationResultService) {
        this.loanAppRepo = loanAppRepo;
        this.personalInfoRepo = personalInfoRepo;
        this.addressRepo = addressRepo;
        this.financialInfoRepo = financialInfoRepo;
        this.employmentRepo = employmentRepo;
        this.debtRepo = debtRepo;
        this.assetRepo = assetRepo;
        this.documentRepo = documentRepo;
        this.creditScoreService = creditScoreService;
        this.userRepo = userRepo;
        this.loanMLService = loanMLService;
        this.loanApplicationResultService = loanApplicationResultService;
    }

    public LoanApplicationResponse processLoanApplication(Long userId, LoanApplicationRequest request) {
        validateRequest(request);

        try {
            // Log the request for debugging
            System.out.println("Processing loan application for user ID: " + userId);
            System.out.println("Loan details: " + request.getLoanDetails().getProductType() + 
                              ", Amount: " + request.getLoanDetails().getRequestedAmount());
            
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new LoanApplicationException("User not found: " + userId, HttpStatus.NOT_FOUND));

            // 1. Save base application with SUBMITTED status
            LoanApplication application = saveLoanApplication(user, request);
            System.out.println("Saved base loan application with ID: " + application.getId());

            // 2. Save personal information
            savePersonalInformation(application, request.getPersonalInformation());
            System.out.println("Saved personal information for application ID: " + application.getId());

            // 3. Save financial information
            FinancialInfo financialInfo = saveFinancialInformation(application, request.getFinancialInformation());
            System.out.println("Saved financial information with ID: " + financialInfo.getId());

            // 4. Prepare and calculate credit score
            Map<String, Object> creditData = prepareCreditData(request, financialInfo);
            
            // 5. Calculate credit score and get decision factors
            Map<String, Object> creditEvaluation = creditScoreService.calculateCreditScore(creditData, financialInfo);
            System.out.println("Calculated credit score: " + 
                             (creditEvaluation.containsKey("creditScore") ? creditEvaluation.get("creditScore") : "Not available"));
            
            // 6. Store the credit evaluation data for later use
            application.setCreditEvaluationData(creditEvaluation);
            application = loanAppRepo.save(application);
            System.out.println("Updated application with credit evaluation data");
            
            // 7. Update financial info with system-generated credit score and save again
            if (creditEvaluation.containsKey("creditScore")) {
                int systemCreditScore = ((Number) creditEvaluation.get("creditScore")).intValue();
                financialInfo.setSystemCreditScore(systemCreditScore);
                
                if (creditEvaluation.containsKey("eligibilityScore")) {
                    int eligibilityScore = ((Number) creditEvaluation.get("eligibilityScore")).intValue();
                    financialInfo.setEligibilityScore(eligibilityScore);
                }
                
                // Also set the application credit score for consistency
                application.setCreditScore((double) systemCreditScore);
                loanAppRepo.save(application);
                
                // Update additional financial metrics
                if (creditEvaluation.containsKey("dti")) {
                    double dti = ((Number) creditEvaluation.get("dti")).doubleValue();
                    financialInfo.setDebtToIncomeRatio(BigDecimal.valueOf(dti));
                }
                
                if (creditEvaluation.containsKey("creditUtilization")) {
                    double utilization = ((Number) creditEvaluation.get("creditUtilization")).doubleValue();
                    financialInfo.setCreditUtilization(BigDecimal.valueOf(utilization));
                }
                
                financialInfoRepo.save(financialInfo);
                System.out.println("Updated financial info with system credit score: " + systemCreditScore);
            }
            
            // 8. Save documents
            if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
                saveApplicationDocuments(application, request.getDocuments());
                System.out.println("Saved " + request.getDocuments().size() + " documents");
            } else {
                System.out.println("No documents to save");
            }

            // 9. Process with ML service (new step)
            try {
                System.out.println("Attempting to process application with ML service");
                
                // Capture the application ID before the lambda
                final Long appId = application.getId();
                
                // Get required objects
                PersonalInfo personalInfo = personalInfoRepo.findByLoanApplicationId(appId)
                        .orElseThrow(() -> new LoanApplicationException(
                                "Personal info not found for application: " + appId, 
                                HttpStatus.NOT_FOUND));
                
                if (loanMLService != null) {
                    // Process with ML
                    System.out.println("Processing application through ML service");
                    application = processApplicationWithML(appId, personalInfo, financialInfo, loanMLService);
                    System.out.println("Successfully processed application with ML service");
                    
                    // Generate loan application result (this is now done inside processApplicationWithML)
                } else {
                    System.out.println("LoanMLService is null - cannot process with ML");
                    
                    // Try to generate a result using just the credit score data
                    try {
                        System.out.println("Attempting to generate loan application result without ML data");
                        loanApplicationResultService.generateLoanApplicationResult(appId);
                        System.out.println("Loan application result generated successfully");
                    } catch (Exception e) {
                        System.out.println("Error generating loan application result: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error during ML processing: " + e.getMessage());
                e.printStackTrace();
                
                // Try to generate a result using just the credit score data even if ML processing failed
                try {
                    System.out.println("Attempting to generate loan application result after ML processing failure");
                    loanApplicationResultService.generateLoanApplicationResult(application.getId());
                    System.out.println("Loan application result generated successfully despite ML failure");
                } catch (Exception resultError) {
                    System.out.println("Error generating loan application result: " + resultError.getMessage());
                    resultError.printStackTrace();
                }
            }

            return buildSuccessResponse(application);

        } catch (LoanApplicationException e) {
            System.out.println("Loan application exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.out.println("Unexpected error in loan application: " + e.getMessage());
            e.printStackTrace();
            throw new LoanApplicationException("Processing failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateRequest(LoanApplicationRequest request) {
        if (request.getLoanDetails() == null ||
                request.getPersonalInformation() == null ||
                request.getFinancialInformation() == null) {
            throw new LoanApplicationException("Invalid request structure", HttpStatus.BAD_REQUEST);
        }

        if (request.getLoanDetails().getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new LoanApplicationException("Requested amount must be positive", HttpStatus.BAD_REQUEST);
        }
    }

    private LoanApplication saveLoanApplication(User user, LoanApplicationRequest request) {
        LoanApplication application = new LoanApplication();
        application.setUser(user);
        application.setProductType(request.getLoanDetails().getProductType());
        application.setRequestedAmount(request.getLoanDetails().getRequestedAmount());
        application.setPurposeDescription(request.getLoanDetails().getPurposeDescription());
        application.setRequestedTermMonths(request.getLoanDetails().getRequestedTermMonths());
        application.setStatus("SUBMITTED");
        return loanAppRepo.save(application);
    }

    private void savePersonalInformation(LoanApplication application,
                                         LoanApplicationRequest.PersonalInformation personalInfoDto) {
        // Save address first
        Address address = new Address();
        address.setStreetAddress(personalInfoDto.getCurrentAddress().getStreetAddress());
        address.setCity(personalInfoDto.getCurrentAddress().getCity());
        address.setProvince(personalInfoDto.getCurrentAddress().getProvince());
        address.setPostalCode(personalInfoDto.getCurrentAddress().getPostalCode());
        address.setCountry(personalInfoDto.getCurrentAddress().getCountry());
        address.setDurationMonths(personalInfoDto.getCurrentAddress().getDurationAtAddressMonths());
        Address savedAddress = addressRepo.save(address);

        // Save personal info
        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.setLoanApplication(application);
        personalInfo.setFirstName(personalInfoDto.getFirstName());
        personalInfo.setLastName(personalInfoDto.getLastName());
        personalInfo.setEmail(personalInfoDto.getEmailAddress());
        personalInfo.setPhoneNumber(personalInfoDto.getPhoneNumber());
        personalInfo.setDateOfBirth(personalInfoDto.getDateOfBirth());
        personalInfo.setAddress(savedAddress);
        personalInfoRepo.save(personalInfo);
    }

    private FinancialInfo saveFinancialInformation(LoanApplication application,
                                                   LoanApplicationRequest.FinancialInformation financialInfoDto) {
        // Save main financial info
        FinancialInfo financialInfo = new FinancialInfo();
        financialInfo.setLoanApplication(application);
        financialInfo.setUser(application.getUser()); // Set user from the application
        financialInfo.setMonthlyIncome(financialInfoDto.getMonthlyIncome());
        financialInfo.setMonthlyExpenses(financialInfoDto.getMonthlyExpenses());
        financialInfo.setCreditScore(financialInfoDto.getCreditScore());
        financialInfo.setEstimatedDebts(financialInfoDto.getEstimatedDebts());
        financialInfo.setCurrentCreditLimit(financialInfoDto.getCurrentCreditLimit());
        financialInfo.setCreditTotalUsage(financialInfoDto.getCreditTotalUsage());

        // Calculate credit utilization
        if (financialInfoDto.getCurrentCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal utilization = financialInfoDto.getCreditTotalUsage()
                    .divide(financialInfoDto.getCurrentCreditLimit(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            financialInfo.setCreditUtilization(utilization);
        } else {
            financialInfo.setCreditUtilization(BigDecimal.ZERO);
        }

        // Save temporarily to get an ID for relationships
        FinancialInfo savedFinancialInfo = financialInfoRepo.save(financialInfo);

        // Save employment history
        saveEmploymentHistory(savedFinancialInfo, financialInfoDto.getEmploymentDetails());

        // Save debts
        saveDebts(savedFinancialInfo, financialInfoDto.getExistingDebts());

        // Save assets
        saveAssets(savedFinancialInfo, financialInfoDto.getAssets());

        // Calculate totalDebts (sum of all debts in the list)
        BigDecimal totalDebts = BigDecimal.ZERO;
        for (LoanApplicationRequest.FinancialInformation.ExistingDebt debt : financialInfoDto.getExistingDebts()) {
            totalDebts = totalDebts.add(debt.getOutstandingAmount());
        }
        savedFinancialInfo.setTotalDebts(totalDebts);

        // Calculate totalAssets (sum of all assets in the list)
        BigDecimal totalAssets = BigDecimal.ZERO;
        for (LoanApplicationRequest.FinancialInformation.Asset asset : financialInfoDto.getAssets()) {
            totalAssets = totalAssets.add(asset.getEstimatedValue());
        }
        savedFinancialInfo.setTotalAssets(totalAssets);

        // Calculate debt-to-income ratio using the provided formula
        // dtiScore(double income, double expenses, double debt, double loanRequest)
        // (expenses + debt + 0.03 * loanRequest) / (income * 12)
        // Note: debt = estimatedDebts + totalDebts
        BigDecimal monthlyIncome = savedFinancialInfo.getMonthlyIncome();
        BigDecimal monthlyExpenses = savedFinancialInfo.getMonthlyExpenses();
        BigDecimal combinedDebt = savedFinancialInfo.getEstimatedDebts().add(totalDebts);
        BigDecimal loanRequest = application.getRequestedAmount();

        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal loanImpact = loanRequest.multiply(BigDecimal.valueOf(0.03));
            BigDecimal numerator = monthlyExpenses.add(combinedDebt).add(loanImpact);
            BigDecimal annualIncome = monthlyIncome.multiply(BigDecimal.valueOf(12));
            BigDecimal dti = numerator.divide(annualIncome, 4, RoundingMode.HALF_UP);
            savedFinancialInfo.setDebtToIncomeRatio(dti);
        } else {
            savedFinancialInfo.setDebtToIncomeRatio(BigDecimal.ONE); // Set to 1.0 if income is zero
        }

        // Save updated financial info with all calculations
        return financialInfoRepo.save(savedFinancialInfo);
    }

    private void saveEmploymentHistory(
            FinancialInfo financialInfo,
            List<LoanApplicationRequest.FinancialInformation.EmploymentDetail> employmentDetails
    )  {
        List<EmploymentHistory> histories = employmentDetails.stream()
                .map(dto -> {
                    EmploymentHistory history = new EmploymentHistory();
                    history.setFinancialInfo(financialInfo);
                    history.setEmployerName(dto.getEmployerName());
                    history.setPosition(dto.getPosition());
                    history.setStartDate(dto.getStartDate());
                    history.setEndDate(dto.getEndDate());
                    history.setEmploymentType(dto.getEmploymentType());
                    history.setDurationMonths(dto.getEmploymentDurationMonths());
                    return history;
                })
                .toList();
        employmentRepo.saveAll(histories);
    }

    private void saveDebts(FinancialInfo financialInfo,
                           List<LoanApplicationRequest.FinancialInformation.ExistingDebt> debts) {
        List<Debt> debtEntities = debts.stream()
                .map(dto -> {
                    Debt debt = new Debt();
                    debt.setFinancialInfo(financialInfo);
                    debt.setDebtType(dto.getDebtType());
                    debt.setOutstandingAmount(dto.getOutstandingAmount());
                    debt.setInterestRate(dto.getInterestRate());
                    debt.setMonthlyPayment(dto.getMonthlyPayment());
                    debt.setRemainingTerm(dto.getRemainingTermMonths());
                    debt.setLender(dto.getLender());
                    debt.setPaymentHistory(dto.getPaymentHistory());
                    return debt;
                })
                .toList();
        debtRepo.saveAll(debtEntities);
    }

    private void saveAssets(FinancialInfo financialInfo,
                            List<LoanApplicationRequest.FinancialInformation.Asset> assets) {
        List<Asset> assetEntities = assets.stream()
                .map(dto -> {
                    Asset asset = new Asset();
                    asset.setFinancialInfo(financialInfo);
                    asset.setAssetType(dto.getAssetType());
                    asset.setDescription(dto.getDescription());
                    asset.setEstimatedValue(dto.getEstimatedValue());
                    return asset;
                })
                .toList();
        assetRepo.saveAll(assetEntities);
    }

    private void saveApplicationDocuments(LoanApplication application,
                                          List<LoanApplicationRequest.DocumentRequest> documents) {
        List<Document> documentEntities = documents.stream()
                .map(dto -> {
                    validateDocument(dto);
                    Document doc = new Document();
                    doc.setLoanApplication(application);
                    doc.setDocumentType(dto.getDocumentType());
                    doc.setFileData(dto.getFile());
                    return doc;
                })
                .toList();
        documentRepo.saveAll(documentEntities);
    }

    private LocalDate parseDate(String dateString) {
        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            throw new LoanApplicationException("Invalid date format. Use YYYY-MM-DD", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateDocument(LoanApplicationRequest.DocumentRequest doc) {
        if (doc.getFile() == null || doc.getFile().isEmpty()) {
            throw new LoanApplicationException("Document file cannot be empty", HttpStatus.BAD_REQUEST);
        }
        if (doc.getDocumentType() == null || doc.getDocumentType().isEmpty()) {
            throw new LoanApplicationException("Document type is required", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Object> prepareCreditData(LoanApplicationRequest request, FinancialInfo financialInfo) {
        Map<String, Object> creditData = new HashMap<>();
        
        // Print for debugging
        System.out.println("Preparing credit data from financial info ID: " + 
                          (financialInfo != null ? financialInfo.getId() : "null"));
        
        try {
            // First, get loan details - no null checks needed as these come from the request
            creditData.put("loanType", request.getLoanDetails().getProductType());
            creditData.put("loanRequest", safeDoubleValue(request.getLoanDetails().getRequestedAmount()));
            creditData.put("requestedAmount", safeDoubleValue(request.getLoanDetails().getRequestedAmount()));
            creditData.put("tenure", request.getLoanDetails().getRequestedTermMonths());
            creditData.put("requestedTermMonths", request.getLoanDetails().getRequestedTermMonths());
            
            // Then extract financial info with null checks
            if (financialInfo != null) {
                // Core financial data
                creditData.put("monthlyIncome", safeDoubleValue(financialInfo.getMonthlyIncome()));
                creditData.put("income", safeDoubleValue(financialInfo.getMonthlyIncome()));
                
                creditData.put("monthlyExpenses", safeDoubleValue(financialInfo.getMonthlyExpenses()));
                creditData.put("expenses", safeDoubleValue(financialInfo.getMonthlyExpenses()));
                
                creditData.put("estimatedDebts", safeDoubleValue(financialInfo.getEstimatedDebts()));
                creditData.put("debt", safeDoubleValue(financialInfo.getEstimatedDebts()));
                
                // Credit usage and limits
                creditData.put("creditTotalUsage", safeDoubleValue(financialInfo.getCreditTotalUsage()));
                creditData.put("usedCredit", safeDoubleValue(financialInfo.getCreditTotalUsage()));
                
                creditData.put("currentCreditLimit", safeDoubleValue(financialInfo.getCurrentCreditLimit()));
                creditData.put("creditLimit", safeDoubleValue(financialInfo.getCurrentCreditLimit()));
                
                // Additional financial data
                creditData.put("totalDebts", safeDoubleValue(financialInfo.getTotalDebts()));
                creditData.put("creditUtilization", safeDoubleValue(financialInfo.getCreditUtilization()));
                creditData.put("totalAssets", safeDoubleValue(financialInfo.getTotalAssets()));
                creditData.put("assets", safeDoubleValue(financialInfo.getTotalAssets()));
                
                // Process payment history
                String paymentHistory = analyzePaymentHistory(financialInfo.getExistingDebts());
                creditData.put("paymentHistory", paymentHistory);
                
                // Calculate employment data
                List<EmploymentHistory> employments = financialInfo.getEmploymentDetails();
                if (employments != null && !employments.isEmpty()) {
                    // Get total employment duration
                    int totalMonthsEmployed = employments.stream()
                            .mapToInt(EmploymentHistory::getDurationMonths)
                            .sum();
                    creditData.put("monthsEmployed", totalMonthsEmployed);
                    
                    // Determine current employment status
                    String primaryEmploymentType = employments.stream()
                            .filter(e -> e.getEndDate() == null)
                            .findFirst()
                            .map(EmploymentHistory::getEmploymentType)
                            .orElse(employments.get(0).getEmploymentType());
                    creditData.put("employmentStatus", primaryEmploymentType);
                    
                    // Estimate credit age based on employment duration
                    // Assume credit history is roughly aligned with employment
                    creditData.put("creditAge", Math.max(totalMonthsEmployed, 6));
                } else {
                    // Default values if no employment data
                    creditData.put("monthsEmployed", 0);
                    creditData.put("employmentStatus", "Unemployed");
                    creditData.put("creditAge", 6); // Minimum credit age
                }
                
                // Extract debt types
                Set<String> debtTypes = new HashSet<>();
                List<Debt> debts = financialInfo.getExistingDebts();
                if (debts != null && !debts.isEmpty()) {
                    debtTypes = debts.stream()
                            .map(Debt::getDebtType)
                            .filter(type -> type != null && !type.isEmpty())
                            .collect(Collectors.toSet());
                    
                    // If debt types are empty but debt exists, add generic type
                    if (debtTypes.isEmpty() && safeDoubleValue(financialInfo.getEstimatedDebts()) > 0) {
                        debtTypes.add("Personal Loan");
                    }
                }
                creditData.put("debtTypes", debtTypes);
                
                // Bank account info
                creditData.put("bankAccounts", financialInfo.getBankAccounts() != null ? 
                               financialInfo.getBankAccounts() : 1);
            } else {
                // Provide reasonable defaults if financial info is missing
                System.out.println("WARNING: Financial info is null, using default values");
                creditData.put("income", 3000.0);
                creditData.put("expenses", 1500.0);
                creditData.put("debt", 0.0);
                creditData.put("usedCredit", 0.0);
                creditData.put("creditLimit", 1000.0);
                creditData.put("paymentHistory", "On-time");
                creditData.put("employmentStatus", "Unemployed");
                creditData.put("monthsEmployed", 0);
                creditData.put("creditAge", 6);
                creditData.put("assets", 0.0);
                creditData.put("bankAccounts", 1);
                creditData.put("debtTypes", new HashSet<>());
            }
        } catch (Exception e) {
            System.out.println("Error preparing credit data: " + e.getMessage());
            e.printStackTrace();
            
            // Ensure minimum required data is present for calculation
            if (!creditData.containsKey("loanRequest")) {
                creditData.put("loanRequest", safeDoubleValue(request.getLoanDetails().getRequestedAmount()));
            }
            if (!creditData.containsKey("income")) creditData.put("income", 3000.0);
            if (!creditData.containsKey("expenses")) creditData.put("expenses", 1500.0);
            if (!creditData.containsKey("debt")) creditData.put("debt", 0.0);
            if (!creditData.containsKey("usedCredit")) creditData.put("usedCredit", 0.0);
            if (!creditData.containsKey("creditLimit")) creditData.put("creditLimit", 1000.0);
            if (!creditData.containsKey("paymentHistory")) creditData.put("paymentHistory", "On-time");
            if (!creditData.containsKey("employmentStatus")) creditData.put("employmentStatus", "Unemployed");
            if (!creditData.containsKey("monthsEmployed")) creditData.put("monthsEmployed", 0);
        }
        
        // Print out what we're sending to help debug
        System.out.println("Prepared credit data: " + creditData);
        
        return creditData;
    }
    
    /**
     * Safely convert BigDecimal to double with null check
     */
    private double safeDoubleValue(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    /**
     * Analyze payment history from debt details and determine overall payment status
     * @param debts List of debt entities
     * @return Overall payment history status (On-time, Late < 30, Late 30-60, Late > 60)
     */
    private String analyzePaymentHistory(List<Debt> debts) {
        if (debts == null || debts.isEmpty()) {
            return "On-time"; // Default if no debt history
        }
        
        // Track the "worst" payment history status
        String worstStatus = "On-time";
        
        for (Debt debt : debts) {
            String status = debt.getPaymentHistory();
            if (status == null) {
                continue;
            }
            
            // Evaluate severity of payment history
            if (status.equals("Late > 60") || status.contains("60+")) {
                return "Late > 60"; // Immediately return the worst status
            } else if ((status.equals("Late 30-60") || status.contains("30-60")) && !worstStatus.equals("Late > 60")) {
                worstStatus = "Late 30-60";
            } else if ((status.equals("Late < 30") || status.contains("< 30")) && 
                      !worstStatus.equals("Late > 60") && !worstStatus.equals("Late 30-60")) {
                worstStatus = "Late < 30";
            }
        }
        
        return worstStatus;
    }

    private LoanApplicationResponse buildSuccessResponse(LoanApplication application) {
        LoanApplicationResponse response = new LoanApplicationResponse(
                application.getId().toString(),
                "Loan application processed successfully",
                application.getStatus()
        );
        
        // Add credit score to response, properly cast Double to int
        Double creditScore = application.getCreditScore();
        if (creditScore != null) {
            response.setCreditScore(creditScore.intValue());
        }
        
        return response;
    }

    /**
     * Process a submitted loan application with the ML model
     * 
     * @param applicationId The ID of the loan application to process
     * @param personalInfo Personal information related to the application
     * @param financialInfo Financial information related to the application
     * @param mlService The ML service to use for processing
     * @return The processed loan application
     */
    @Transactional
    public LoanApplication processApplicationWithML(
            Long applicationId, 
            PersonalInfo personalInfo,
            FinancialInfo financialInfo,
            LoanMLService mlService) {
        
        LoanApplication application = loanAppRepo.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationException("Loan application not found: " + applicationId, HttpStatus.NOT_FOUND));
        
        // Verify that the application is in SUBMITTED status
        if (!"SUBMITTED".equals(application.getStatus())) {
            throw new LoanApplicationException("Application must be in SUBMITTED status for ML processing", HttpStatus.BAD_REQUEST);
        }
        
        // Call ML service for decision
        Map<String, Object> mlDecision = mlService.getLoanDecision(application, financialInfo, personalInfo);
        
        // Apply decision to application
        application = mlService.applyMLDecision(application, mlDecision);
        
        // Save the updated application
        application = loanAppRepo.save(application);
        
        // Generate loan application result
        try {
            System.out.println("Generating loan application result for application ID: " + applicationId);
            loanApplicationResultService.generateLoanApplicationResult(applicationId);
            System.out.println("Loan application result generated successfully");
        } catch (Exception e) {
            System.out.println("Error generating loan application result: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the processing if result generation fails
        }
        
        return application;
    }

    /**
     * Find and process all loan applications with SUBMITTED status
     * 
     * @param mlService The ML service to use for processing
     * @return Number of applications processed
     */
    @Transactional
    public int processAllSubmittedApplications(LoanMLService mlService) {
        List<LoanApplication> submittedApplications = loanAppRepo.findByStatus("SUBMITTED");
        
        if (submittedApplications.isEmpty()) {
            return 0;
        }
        
        int processedCount = 0;
        
        for (LoanApplication application : submittedApplications) {
            try {
                // Capture the application ID in a final variable
                final Long applicationId = application.getId();
                
                // Get required entities
                PersonalInfo personalInfo = personalInfoRepo.findByLoanApplicationId(applicationId)
                        .orElseThrow(() -> new LoanApplicationException(
                                "Personal info not found for application: " + applicationId, 
                                HttpStatus.NOT_FOUND));
                
                FinancialInfo financialInfo = financialInfoRepo.findByLoanApplicationId(applicationId)
                        .orElseThrow(() -> new LoanApplicationException(
                                "Financial info not found for application: " + applicationId, 
                                HttpStatus.NOT_FOUND));
                
                // Process with ML - this will now also generate the loan application result
                processApplicationWithML(applicationId, personalInfo, financialInfo, mlService);
                
                processedCount++;
            } catch (Exception e) {
                // Capture application ID in a final variable for logging and error handling
                final Long applicationId = application.getId();
                
                // Log the error but continue processing other applications
                System.out.println("Error processing application " + applicationId + ": " + e.getMessage());
                application.setStatus("PROCESSING_ERROR");
                application.setCreditEvaluationData(Map.of("error", e.getMessage()));
                loanAppRepo.save(application);
                
                // Try to generate a result even if the application had an error
                try {
                    loanApplicationResultService.generateLoanApplicationResult(applicationId);
                } catch (Exception resultError) {
                    System.out.println("Error generating result for application " + applicationId + 
                                      " after processing error: " + resultError.getMessage());
                }
            }
        }
        
        return processedCount;
    }

    /**
     * Get a loan application by ID
     * 
     * @param applicationId The ID of the loan application
     * @return The loan application or null if not found
     */
    public LoanApplication getLoanApplication(Long applicationId) {
        return loanAppRepo.findById(applicationId).orElse(null);
    }

    /**
     * Get personal info for a loan application
     * 
     * @param applicationId The loan application ID
     * @return The personal info or null if not found
     */
    public PersonalInfo getPersonalInfo(Long applicationId) {
        return personalInfoRepo.findByLoanApplicationId(applicationId).orElse(null);
    }

    /**
     * Get financial info for a loan application
     * 
     * @param applicationId The loan application ID
     * @return The financial info or null if not found
     */
    public FinancialInfo getFinancialInfo(Long applicationId) {
        return financialInfoRepo.findByLoanApplicationId(applicationId).orElse(null);
    }
}