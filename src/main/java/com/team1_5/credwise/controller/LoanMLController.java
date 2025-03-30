package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.FinancialInfo;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.LoanApplicationResult;
import com.team1_5.credwise.model.PersonalInfo;
import com.team1_5.credwise.repository.FinancialInfoRepository;
import com.team1_5.credwise.repository.LoanApplicationRepository;
import com.team1_5.credwise.repository.PersonalInfoRepository;
import com.team1_5.credwise.service.LoanApplicationResultService;
import com.team1_5.credwise.service.LoanMLService;
import com.team1_5.credwise.util.CanadianCreditScoringSystem;
import com.team1_5.credwise.util.CreditScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loan-applications")
public class LoanMLController {

    private static final Logger logger = LoggerFactory.getLogger(LoanMLController.class);
    private final LoanMLService loanMLService;
    private final LoanApplicationRepository loanApplicationRepository;
    private final FinancialInfoRepository financialInfoRepository;
    private final PersonalInfoRepository personalInfoRepository;
    private final CreditScoreService creditScoreService;
    private final LoanApplicationResultService loanApplicationResultService;

    public LoanMLController(
            LoanMLService loanMLService,
            LoanApplicationRepository loanApplicationRepository,
            FinancialInfoRepository financialInfoRepository,
            PersonalInfoRepository personalInfoRepository,
            CreditScoreService creditScoreService,
            LoanApplicationResultService loanApplicationResultService) {
        this.loanMLService = loanMLService;
        this.loanApplicationRepository = loanApplicationRepository;
        this.financialInfoRepository = financialInfoRepository;
        this.personalInfoRepository = personalInfoRepository;
        this.creditScoreService = creditScoreService;
        this.loanApplicationResultService = loanApplicationResultService;
    }

    /**
     * Process a loan application through the ML model
     * 
     * @param applicationId The loan application ID
     * @return Response with the application status and decision details
     */
    @PostMapping("/{applicationId}/process-ml")
    @Transactional
    public ResponseEntity<Map<String, Object>> processApplicationWithML(@PathVariable Long applicationId) {
        // Find application
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found: " + applicationId));
        
        // Get related financial info
        FinancialInfo financialInfo = financialInfoRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial info not found for application: " + applicationId));
        
        // Get related personal info
        PersonalInfo personalInfo = personalInfoRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal info not found for application: " + applicationId));
        
        // 1. Calculate credit score using the comprehensive scoring system
        Map<String, Object> creditData = prepareCreditData(application, financialInfo);
        Map<String, Object> creditEvaluation = creditScoreService.calculateCreditScore(creditData, financialInfo);
        
        // 2. Call ML service only for specific decision factors
        Map<String, Object> mlDecision = loanMLService.getLoanDecision(application, financialInfo, personalInfo);
        
        // 3. Combine credit evaluation and ML decision data
        Map<String, Object> combinedDecision = new HashMap<>(creditEvaluation);
        
        // Only take specific fields from ML model
        if (mlDecision.containsKey("approval_probability")) {
            combinedDecision.put("approval_probability", mlDecision.get("approval_probability"));
        }
        if (mlDecision.containsKey("is_approved")) {
            combinedDecision.put("is_approved", mlDecision.get("is_approved"));
        }
        if (mlDecision.containsKey("approved_amount")) {
            combinedDecision.put("approved_amount", mlDecision.get("approved_amount"));
        }
        if (mlDecision.containsKey("interest_rate")) {
            combinedDecision.put("interest_rate", mlDecision.get("interest_rate"));
        }
        
        // 4. Update application with combined decision data
        application.setCreditEvaluationData(combinedDecision);
        application = loanMLService.applyMLDecision(application, combinedDecision);
        
        // 5. Save updated application
        application = loanApplicationRepository.save(application);
        
        // 6. Save updated financial info with system credit score and eligibility score
        financialInfo = financialInfoRepository.save(financialInfo);
        
        // 7. Generate loan application result
        try {
            LoanApplicationResult result = loanApplicationResultService.generateLoanApplicationResult(applicationId);
            logger.info("Generated loan application result: {}", result.getId());
        } catch (Exception e) {
            logger.error("Error generating loan application result: {}", e.getMessage(), e);
        }
        
        // 8. Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("applicationId", application.getId());
        response.put("status", application.getStatus());
        response.put("systemCreditScore", financialInfo.getSystemCreditScore());
        response.put("eligibilityScore", financialInfo.getEligibilityScore());
        response.put("decision", combinedDecision);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Prepare credit data for the credit scoring system
     */
    private Map<String, Object> prepareCreditData(LoanApplication application, FinancialInfo financialInfo) {
        Map<String, Object> creditData = new HashMap<>();
        
        try {
            System.out.println("Preparing credit data for ML processing - App ID: " + 
                              application.getId());
            
            // Loan details
            creditData.put("loanType", application.getProductType());
            creditData.put("loanRequest", safeDoubleValue(application.getRequestedAmount()));
            creditData.put("requestedAmount", safeDoubleValue(application.getRequestedAmount()));
            creditData.put("tenure", application.getRequestedTermMonths());
            creditData.put("requestedTermMonths", application.getRequestedTermMonths());
            
            if (financialInfo != null) {
                // Core financial data with unified keys 
                creditData.put("income", safeDoubleValue(financialInfo.getMonthlyIncome()));
                creditData.put("monthlyIncome", safeDoubleValue(financialInfo.getMonthlyIncome()));
                
                creditData.put("expenses", safeDoubleValue(financialInfo.getMonthlyExpenses()));
                creditData.put("monthlyExpenses", safeDoubleValue(financialInfo.getMonthlyExpenses()));
                
                creditData.put("debt", safeDoubleValue(financialInfo.getEstimatedDebts()));
                creditData.put("estimatedDebts", safeDoubleValue(financialInfo.getEstimatedDebts()));
                
                // Credit usage data
                creditData.put("usedCredit", safeDoubleValue(financialInfo.getCreditTotalUsage()));
                creditData.put("creditTotalUsage", safeDoubleValue(financialInfo.getCreditTotalUsage()));
                
                creditData.put("creditLimit", safeDoubleValue(financialInfo.getCurrentCreditLimit()));
                creditData.put("currentCreditLimit", safeDoubleValue(financialInfo.getCurrentCreditLimit()));
                
                // Additional financial metrics
                creditData.put("totalDebts", safeDoubleValue(financialInfo.getTotalDebts()));
                creditData.put("creditUtilization", safeDoubleValue(financialInfo.getCreditUtilization()));
                creditData.put("assets", safeDoubleValue(financialInfo.getTotalAssets()));
                creditData.put("totalAssets", safeDoubleValue(financialInfo.getTotalAssets()));
                
                // Payment history
                String paymentHistory = analyzePaymentHistory(financialInfo.getExistingDebts());
                creditData.put("paymentHistory", paymentHistory);
                
                // Employment data
                if (financialInfo.getEmploymentDetails() != null && !financialInfo.getEmploymentDetails().isEmpty()) {
                    // Calculate total employment duration from all jobs
                    int totalMonthsEmployed = financialInfo.getEmploymentDetails().stream()
                            .mapToInt(employment -> employment.getDurationMonths() != null ? employment.getDurationMonths() : 0)
                            .sum();
                    creditData.put("monthsEmployed", totalMonthsEmployed);
                    
                    // Get current employment status
                    String employmentStatus = financialInfo.getEmploymentDetails().stream()
                            .filter(e -> e.getEndDate() == null)
                            .findFirst()
                            .map(e -> e.getEmploymentType())
                            .orElse(financialInfo.getEmploymentDetails().get(0).getEmploymentType());
                    creditData.put("employmentStatus", employmentStatus);
                    
                    // Estimate credit age based on employment
                    creditData.put("creditAge", Math.max(totalMonthsEmployed, 6));
                    
                    // Log employment details for debugging
                    System.out.println("Employment details: status=" + employmentStatus + ", totalMonths=" + totalMonthsEmployed);
                } else {
                    creditData.put("employmentStatus", "Unemployed");
                    creditData.put("monthsEmployed", 0);
                    creditData.put("creditAge", 6); // Minimum credit age
                }
                
                // Debt types as a Set for consistency
                Set<String> debtTypes = new HashSet<>();
                if (financialInfo.getExistingDebts() != null) {
                    debtTypes = financialInfo.getExistingDebts().stream()
                            .map(debt -> debt.getDebtType())
                            .filter(type -> type != null && !type.isEmpty())
                            .collect(Collectors.toSet());
                    
                    // If no debt types but debt exists, add default
                    if (debtTypes.isEmpty() && safeDoubleValue(financialInfo.getEstimatedDebts()) > 0) {
                        debtTypes.add("Personal Loan");
                    }
                }
                creditData.put("debtTypes", debtTypes);
                
                // Bank accounts
                creditData.put("bankAccounts", financialInfo.getBankAccounts() != null ? 
                              financialInfo.getBankAccounts() : 1);
            } else {
                // Default values if financial info is missing
                System.out.println("WARNING: Financial info is null for application " + 
                                  application.getId() + ", using defaults");
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
                creditData.put("debtTypes", new HashSet<String>());
            }
            
            System.out.println("Prepared credit data for ML: " + creditData);
            
        } catch (Exception e) {
            System.out.println("Error preparing credit data for ML: " + e.getMessage());
            e.printStackTrace();
            
            // Ensure minimum data is present
            if (!creditData.containsKey("loanRequest")) {
                creditData.put("loanRequest", safeDoubleValue(application.getRequestedAmount()));
            }
            if (!creditData.containsKey("income")) creditData.put("income", 3000.0);
            if (!creditData.containsKey("expenses")) creditData.put("expenses", 1500.0);
            if (!creditData.containsKey("debt")) creditData.put("debt", 0.0);
            if (!creditData.containsKey("usedCredit")) creditData.put("usedCredit", 0.0);
            if (!creditData.containsKey("creditLimit")) creditData.put("creditLimit", 1000.0);
            if (!creditData.containsKey("paymentHistory")) creditData.put("paymentHistory", "On-time");
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
     * Analyze payment history from debt details
     */
    private String analyzePaymentHistory(List<com.team1_5.credwise.model.Debt> debts) {
        if (debts == null || debts.isEmpty()) {
            return "On-time"; // Default if no debt history
        }
        
        // Track the "worst" payment history status
        String worstStatus = "On-time";
        
        for (com.team1_5.credwise.model.Debt debt : debts) {
            String status = debt.getPaymentHistory();
            if (status == null) {
                continue;
            }
            
            if (status.contains("> 60") || status.contains("60+")) {
                return "Late > 60"; // Immediately return the worst status
            } else if (status.contains("30-60") && !worstStatus.equals("Late > 60")) {
                worstStatus = "Late 30-60";
            } else if (status.contains("< 30") && 
                      !worstStatus.equals("Late > 60") && !worstStatus.equals("Late 30-60")) {
                worstStatus = "Late < 30";
            }
        }
        
        return worstStatus;
    }
} 