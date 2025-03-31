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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.service.LoanApplicationService;
import com.team1_5.credwise.dto.LoanMLResponse;

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
@RequestMapping("/api/ml")
public class LoanMLController {

    private static final Logger logger = LoggerFactory.getLogger(LoanMLController.class);
    private final LoanMLService loanMLService;
    private final LoanApplicationRepository loanApplicationRepository;
    private final FinancialInfoRepository financialInfoRepository;
    private final PersonalInfoRepository personalInfoRepository;
    private final CreditScoreService creditScoreService;
    private final LoanApplicationResultService loanApplicationResultService;
    private final LoanApplicationService loanApplicationService;

    public LoanMLController(
            LoanMLService loanMLService,
            LoanApplicationRepository loanApplicationRepository,
            FinancialInfoRepository financialInfoRepository,
            PersonalInfoRepository personalInfoRepository,
            CreditScoreService creditScoreService,
            LoanApplicationResultService loanApplicationResultService,
            LoanApplicationService loanApplicationService) {
        this.loanMLService = loanMLService;
        this.loanApplicationRepository = loanApplicationRepository;
        this.financialInfoRepository = financialInfoRepository;
        this.personalInfoRepository = personalInfoRepository;
        this.creditScoreService = creditScoreService;
        this.loanApplicationResultService = loanApplicationResultService;
        this.loanApplicationService = loanApplicationService;
    }

    /**
     * Process a loan application through the ML model
     * 
     * @param applicationId The loan application ID
     * @return Response with the application status and decision details
     */
    @PostMapping("/process/{applicationId}")
    public ResponseEntity<?> processApplication(@PathVariable Long applicationId) {
        try {
            LoanApplication application = loanApplicationService.getLoanApplication(applicationId);
            FinancialInfo financialInfo = application.getFinancialInfo();
            PersonalInfo personalInfo = application.getPersonalInfo();
            
            if (financialInfo == null || personalInfo == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Financial information or personal information is missing");
            }
            
            // Get credit evaluation data
            Map<String, Object> creditData = loanApplicationService.prepareCreditData(application);
            
            // Get ML decision
            Map<String, Object> mlDecision = loanMLService.getLoanDecision(application, financialInfo, personalInfo);
            
            // Combine ML decision with credit data
            Map<String, Object> combinedDecision = new HashMap<>();
            combinedDecision.putAll(mlDecision);
            combinedDecision.putAll(creditData);
            
            // Create response
            LoanMLResponse response = new LoanMLResponse(
                applicationId,
                application.getStatus(),
                financialInfo.getSystemCreditScore(),
                financialInfo.getEligibilityScore(),
                combinedDecision
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing application: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing application: " + e.getMessage());
        }
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