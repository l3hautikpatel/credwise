package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.exception.LoanApplicationException;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.*;
import com.team1_5.credwise.repository.*;
import com.team1_5.credwise.util.CreditScoreService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(LoanApplicationService.class);

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
            // Log the full request JSON for debugging data flow
            System.out.println("\n======== ORIGINAL API REQUEST ========");
            System.out.println("User ID: " + userId);
            System.out.println("Loan Type: " + request.getLoanDetails().getProductType());
            System.out.println("Requested Amount: " + request.getLoanDetails().getRequestedAmount());
            System.out.println("Term Months: " + request.getLoanDetails().getRequestedTermMonths());
            
            // Log detailed financial information
            System.out.println("\nFINANCIAL INFORMATION:");
            System.out.println("Monthly Income: " + request.getFinancialInformation().getMonthlyIncome());
            System.out.println("Monthly Expenses: " + request.getFinancialInformation().getMonthlyExpenses());
            System.out.println("Estimated Debts: " + request.getFinancialInformation().getEstimatedDebts());
            System.out.println("Credit Score: " + request.getFinancialInformation().getCreditScore());
            System.out.println("Credit Limit: " + request.getFinancialInformation().getCurrentCreditLimit());
            System.out.println("Credit Usage: " + request.getFinancialInformation().getCreditTotalUsage());
            
            // Calculate and log the credit utilization
            if (request.getFinancialInformation().getCurrentCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal utilization = request.getFinancialInformation().getCreditTotalUsage()
                    .divide(request.getFinancialInformation().getCurrentCreditLimit(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                System.out.println("🔴 Calculated Credit Utilization: " + utilization + "%");
                
                // Alert if over 100%
                if (utilization.compareTo(BigDecimal.valueOf(100)) > 0) {
                    System.out.println("⚠️ WARNING: Credit utilization is over 100%!");
                }
            } else {
                System.out.println("Credit Limit is zero, cannot calculate utilization");
            }
            
            System.out.println("Employment Duration: " + 
                (request.getFinancialInformation().getEmploymentDetails() != null && 
                 !request.getFinancialInformation().getEmploymentDetails().isEmpty() ? 
                 request.getFinancialInformation().getEmploymentDetails().get(0).getEmploymentDurationMonths() : "N/A"));
            
            // Continue with normal processing
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
            Map<String, Object> creditData = prepareCreditData(application);
            System.out.println("\n======== DATA SENT TO CREDIT SCORE CALCULATION ========");
            System.out.println(mapToDebugString(creditData));
            
            // 5. Calculate credit score and get decision factors
            Map<String, Object> creditEvaluation = creditScoreService.calculateCreditScore(creditData, financialInfo);
            System.out.println("\n======== CREDIT SCORE CALCULATION RESULT ========");
            System.out.println(mapToDebugString(creditEvaluation));
            
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

            // 9. Process with ML service - THIS IS THE PRIMARY DECISION POINT
            boolean mlProcessingAttempted = false;
            boolean mlProcessingSuccessful = false;
            
            try {
                System.out.println("Attempting to process application with ML service");
                
                // Get required objects
                final Long appId = application.getId();
                PersonalInfo personalInfo = personalInfoRepo.findByLoanApplicationId(appId)
                        .orElseThrow(() -> new LoanApplicationException(
                                "Personal info not found for application: " + appId, 
                                HttpStatus.NOT_FOUND));
                
                if (loanMLService != null) {
                    // Process with ML - this is the primary decision maker
                    mlProcessingAttempted = true;
                    System.out.println("Processing application through ML service");
                    
                    // Log what's being sent to ML service by capturing its request data preparation
                    // This is temporary code to debug the request
                    Map<String, Object> debugMLRequestData = loanMLService.debugPrepareMLRequestData(application, financialInfo, personalInfo);
                    System.out.println("\n======== DATA PREPARED FOR ML MODEL API ========");
                    System.out.println(mapToDebugString(debugMLRequestData));
                    
                    // Process normally
                    application = processApplicationWithML(appId, personalInfo, financialInfo, loanMLService);
                    
                    // Check if ML processing was successful (decision made or review needed)
                    mlProcessingSuccessful = application.getStatus() != null && 
                                         !"SUBMITTED".equals(application.getStatus());
                    
                    System.out.println("ML processing completed with status: " + application.getStatus());
                    
                    // Generate result based on ML decision
                    if ("APPROVED".equals(application.getStatus()) || "DENIED".equals(application.getStatus())) {
                        try {
                            loanApplicationResultService.generateLoanApplicationResult(appId);
                            System.out.println("Generated result based on ML decision");
                        } catch (Exception e) {
                            System.out.println("Failed to generate result: " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("LoanMLService is null - cannot process with ML");
                }
            } catch (Exception e) {
                System.out.println("Error during ML processing: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 10. ONLY if ML processing was not attempted or failed completely, use fallback
            // This is only for cases where ML service is unavailable
            if (!mlProcessingAttempted || (application.getStatus() == null || "SUBMITTED".equals(application.getStatus()))) {
                System.out.println("ML processing was not attempted or failed - using fallback process");
                
                // Set to REVIEW_NEEDED to flag for manual review
                application.setStatus("REVIEW_NEEDED");
                application = loanAppRepo.save(application);
                
                // Try to generate a basic result
                try {
                    loanApplicationResultService.generateLoanApplicationResult(application.getId());
                    System.out.println("Generated basic result with REVIEW_NEEDED status");
                } catch (Exception e) {
                    System.out.println("Failed to generate basic result: " + e.getMessage());
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
        financialInfo.setLoanApplication(application); // Set the loan application
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
            
            // Log warning for high utilization
            if (utilization.compareTo(BigDecimal.valueOf(100)) > 0) {
                System.out.println("⚠️ WARNING: Credit utilization is over 100%: " + utilization + "% - this may negatively impact credit score");
                logger.warn("Credit utilization is over 100%: {}% for application ID: {}", 
                         utilization, application.getId());
            }
        } else {
            // If credit limit is zero, set a default high utilization value
            System.out.println("⚠️ WARNING: Credit limit is zero, setting default utilization value");
            logger.warn("Credit limit is zero for application ID: {}, using default utilization", application.getId());
            financialInfo.setCreditUtilization(BigDecimal.valueOf(100)); // Use 100% as default when limit is invalid
        }

        // Save temporarily to get an ID for relationships
        FinancialInfo savedFinancialInfo = financialInfoRepo.save(financialInfo);

        // Set the bidirectional relationship
        application.setFinancialInfo(savedFinancialInfo);
        application = loanAppRepo.save(application);

        // DEBUG: Print employment details from the DTO before saving
        List<LoanApplicationRequest.FinancialInformation.EmploymentDetail> employmentDetails = 
            financialInfoDto.getEmploymentDetails();
            
        System.out.println("FINANCIAL DEBUG: Processing employments before saving. Count: " + 
            (employmentDetails != null ? employmentDetails.size() : "null"));
        
        if (employmentDetails != null && !employmentDetails.isEmpty()) {
            System.out.println("FINANCIAL DEBUG: First employment duration: " + 
                employmentDetails.get(0).getEmploymentDurationMonths());
        }

        // Save employment history - inline instead of helper method
        if (employmentDetails != null && !employmentDetails.isEmpty()) {
            System.out.println("FINANCIAL DEBUG: Creating and saving employment history records...");
            
            List<EmploymentHistory> histories = new java.util.ArrayList<>();
            
            for (LoanApplicationRequest.FinancialInformation.EmploymentDetail dto : employmentDetails) {
                EmploymentHistory history = new EmploymentHistory();
                history.setFinancialInfo(savedFinancialInfo);
                history.setEmployerName(dto.getEmployerName());
                history.setPosition(dto.getPosition());
                history.setStartDate(dto.getStartDate());
                history.setEndDate(dto.getEndDate());
                history.setEmploymentType(dto.getEmploymentType());
                
                // Make sure we correctly set the duration months and handle potential null values
                Integer durationMonths = dto.getEmploymentDurationMonths();
                if (durationMonths == null && dto.getStartDate() != null) {
                    // Calculate duration from dates if available
                    LocalDate endDate = dto.getEndDate() != null ? dto.getEndDate() : LocalDate.now();
                    durationMonths = calculateMonthsBetween(dto.getStartDate(), endDate);
                    System.out.println("FINANCIAL DEBUG: Calculated duration from dates: " + durationMonths + " months");
                }
                
                history.setDurationMonths(durationMonths);
                
                System.out.println("FINANCIAL DEBUG: Adding employment record with duration: " + 
                    durationMonths + " months, employer: " + dto.getEmployerName());
                
                histories.add(history);
            }
            
            // Save all employment histories
            List<EmploymentHistory> savedHistories = employmentRepo.saveAll(histories);
            System.out.println("FINANCIAL DEBUG: Saved " + savedHistories.size() + " employment records");
            
            // Verify they were saved correctly
            List<EmploymentHistory> verifyEmps = employmentRepo.findByFinancialInfoId(savedFinancialInfo.getId());
            System.out.println("FINANCIAL DEBUG: Verification - Found " + 
                (verifyEmps != null ? verifyEmps.size() : "null") + " employment records for financial info ID " + 
                savedFinancialInfo.getId());
        } else {
            System.out.println("FINANCIAL DEBUG: No employment data to save");
        }

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
        savedFinancialInfo = financialInfoRepo.save(savedFinancialInfo);
        
        // Final verification
        List<EmploymentHistory> finalEmps = employmentRepo.findByFinancialInfoId(savedFinancialInfo.getId());
        System.out.println("FINAL EMPLOYMENT CHECK: Found " + 
            (finalEmps != null ? finalEmps.size() : "null") + " employment records for financial info ID " + 
            savedFinancialInfo.getId());
            
        return savedFinancialInfo;
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

    public Map<String, Object> prepareCreditData(LoanApplication application) {
        Map<String, Object> creditData = new HashMap<>();
        
        FinancialInfo financialInfo = application.getFinancialInfo();
        if (financialInfo == null) {
            throw new ResourceNotFoundException("Financial information not found for application: " + application.getId());
        }
        
        // Add loan details
        creditData.put("loanType", application.getProductType());
        creditData.put("requestedAmount", application.getRequestedAmount());
        creditData.put("requestedTerm", application.getRequestedTermMonths());
        
        // Add financial metrics
        creditData.put("monthlyIncome", financialInfo.getMonthlyIncome());
        creditData.put("monthlyExpenses", financialInfo.getMonthlyExpenses());
        creditData.put("totalDebts", financialInfo.getEstimatedDebts());
        creditData.put("creditUsage", financialInfo.getCreditTotalUsage());
        creditData.put("creditLimit", financialInfo.getCurrentCreditLimit());
        creditData.put("paymentHistory", "On-time"); // Default value
        
        // Add employment data from current employment
        List<EmploymentHistory> employmentDetails = financialInfo.getEmploymentDetails();
        if (employmentDetails != null && !employmentDetails.isEmpty()) {
            // Get current employment (one with no end date)
            EmploymentHistory currentEmployment = employmentDetails.stream()
                .filter(e -> e.getEndDate() == null)
                .findFirst()
                .orElse(employmentDetails.get(0)); // If no current employment, use the most recent one
            
            creditData.put("employmentStatus", currentEmployment.getEmploymentType());
            creditData.put("monthsEmployed", currentEmployment.getDurationMonths());
        } else {
            creditData.put("employmentStatus", "Unemployed");
            creditData.put("monthsEmployed", 0);
        }
        
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
        
        try {
            // 1. Get decision from ML API - this is the primary decision maker
            Map<String, Object> mlDecision = mlService.getLoanDecision(application, financialInfo, personalInfo);
            System.out.println("ML API response for application " + applicationId + ": " + mlDecision);
            
            // 2. Apply ML decision directly to the application - no overrides
            application = mlService.applyMLDecision(application, mlDecision);
            
            // 3. Save the application with the ML-determined status
            application = loanAppRepo.save(application);
            System.out.println("Saved application " + applicationId + " with ML-determined status: " + application.getStatus());
            
            // 4. Return the application with ML-determined status
            return application;
            
        } catch (Exception e) {
            System.out.println("Exception during ML processing: " + e.getMessage());
            e.printStackTrace();
            
            // Mark as REVIEW_NEEDED on error
            application.setStatus("REVIEW_NEEDED");
            
            // Record the error
            Map<String, Object> creditEvaluationData = application.getCreditEvaluationData();
            if (creditEvaluationData == null) {
                creditEvaluationData = new HashMap<>();
            }
            creditEvaluationData.put("ml_error", "ML processing exception: " + e.getMessage());
            application.setCreditEvaluationData(creditEvaluationData);
            
            // Save the application with error status
            application = loanAppRepo.save(application);
            return application;
        }
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

    /**
     * Calculate number of months between two dates
     */
    private int calculateMonthsBetween(LocalDate startDate, LocalDate endDate) {
        return (int) (startDate.until(endDate, java.time.temporal.ChronoUnit.MONTHS));
    }

    // Helper method to format a map for debugging output
    private String mapToDebugString(Map<String, Object> map) {
        if (map == null) return "null";
        StringBuilder sb = new StringBuilder();
        map.forEach((key, value) -> {
            sb.append(key).append(": ");
            if (value instanceof BigDecimal) {
                sb.append(((BigDecimal) value).stripTrailingZeros().toPlainString());
            } else {
                sb.append(value);
            }
            sb.append("\n");
        });
        return sb.toString();
    }
}