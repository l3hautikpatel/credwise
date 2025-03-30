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
import java.util.List;
import java.util.Map;

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
        
        // Loan details
        creditData.put("loanType", application.getProductType());
        creditData.put("requestedAmount", application.getRequestedAmount().doubleValue());
        creditData.put("requestedTermMonths", application.getRequestedTermMonths());
        
        // Financial info
        creditData.put("monthlyIncome", financialInfo.getMonthlyIncome().doubleValue());
        creditData.put("monthlyExpenses", financialInfo.getMonthlyExpenses().doubleValue());
        creditData.put("estimatedDebts", financialInfo.getEstimatedDebts().doubleValue());
        creditData.put("totalDebts", financialInfo.getTotalDebts() != null ? 
                financialInfo.getTotalDebts().doubleValue() : 0.0);
        creditData.put("currentCreditLimit", financialInfo.getCurrentCreditLimit().doubleValue());
        creditData.put("creditTotalUsage", financialInfo.getCreditTotalUsage().doubleValue());
        creditData.put("creditUtilization", financialInfo.getCreditUtilization() != null ? 
                financialInfo.getCreditUtilization().doubleValue() : 0.0);
        creditData.put("totalAssets", financialInfo.getTotalAssets() != null ? 
                financialInfo.getTotalAssets().doubleValue() : 0.0);
        
        // Payment history from debt details
        String paymentHistory = analyzePaymentHistory(financialInfo.getExistingDebts());
        creditData.put("paymentHistory", paymentHistory);
        
        // Calculate employment details
        String employmentStatus = "Unemployed";
        int monthsEmployed = 0;
        
        if (financialInfo.getEmploymentDetails() != null && !financialInfo.getEmploymentDetails().isEmpty()) {
            // Get the most recent employment
            employmentStatus = financialInfo.getEmploymentDetails().get(0).getEmploymentType();
            // Sum up months employed across all jobs
            monthsEmployed = financialInfo.getEmploymentDetails().stream()
                    .mapToInt(employment -> employment.getDurationMonths())
                    .sum();
        }
        
        creditData.put("employmentStatus", employmentStatus);
        creditData.put("monthsEmployed", monthsEmployed);
        
        // Extract debt types
        List<String> accountTypes = new ArrayList<>();
        if (financialInfo.getExistingDebts() != null) {
            financialInfo.getExistingDebts().forEach(debt -> 
                accountTypes.add(debt.getDebtType())
            );
        }
        creditData.put("debtTypes", accountTypes);
        creditData.put("bankAccounts", 2); // Default assumption
        
        // For debt ratio calculation
        creditData.put("income", financialInfo.getMonthlyIncome().doubleValue());
        creditData.put("expenses", financialInfo.getMonthlyExpenses().doubleValue());
        creditData.put("debt", financialInfo.getEstimatedDebts().doubleValue());
        creditData.put("loanRequest", application.getRequestedAmount().doubleValue());
        creditData.put("tenure", application.getRequestedTermMonths());
        
        return creditData;
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