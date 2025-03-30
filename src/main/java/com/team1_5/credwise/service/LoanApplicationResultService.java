package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.LoanApplicationResult;
import com.team1_5.credwise.model.FinancialInfo;
import com.team1_5.credwise.model.DecisionFactor;
import com.team1_5.credwise.repository.LoanApplicationRepository;
import com.team1_5.credwise.repository.LoanApplicationResultRepository;
import com.team1_5.credwise.repository.FinancialInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class LoanApplicationResultService {
    private static final Logger logger = LoggerFactory.getLogger(LoanApplicationResultService.class);
    
    private final LoanApplicationResultRepository loanApplicationResultRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final FinancialInfoRepository financialInfoRepository;
    private final DecisionFactorService decisionFactorService;

    public LoanApplicationResultService(
            LoanApplicationResultRepository loanApplicationResultRepository, 
            LoanApplicationRepository loanApplicationRepository,
            FinancialInfoRepository financialInfoRepository,
            DecisionFactorService decisionFactorService) {
        this.loanApplicationResultRepository = loanApplicationResultRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.financialInfoRepository = financialInfoRepository;
        this.decisionFactorService = decisionFactorService;
    }

    @Transactional(readOnly = true)
    public LoanApplicationResultResponse getLoanApplicationResult(Long applicationId) {
        logger.info("Retrieving loan application result for loan application ID: {}", applicationId);
        
        LoanApplicationResult result = loanApplicationResultRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application result not found for loan application ID: " + applicationId));
        
        List<DecisionFactor> decisionFactors = decisionFactorService.getDecisionFactorsByResult(result);
        List<LoanApplicationResultResponse.DecisionFactorResponse> decisionFactorResponses = decisionFactors.stream()
                .map(factor -> new LoanApplicationResultResponse.DecisionFactorResponse(
                        factor.getFactor(),
                        factor.getImpact(),
                        factor.getDescription()))
                .collect(Collectors.toList());
        
        return new LoanApplicationResultResponse(
                result.getStatus(),
                result.getMessage(),
                result.getEligibilityScore(),
                result.getMaxEligibleAmount(),
                result.getSuggestedInterestRate(),
                result.getSuggestedTerm(),
                result.getEstimatedMonthlyPayment(),
                decisionFactorResponses
        );
    }
    
    /**
     * Generate a loan application result based on credit evaluation data
     */
    @Transactional
    public LoanApplicationResultResponse generateLoanApplicationResult(Long applicationId) {
        logger.info("Generating loan application result for application ID: {}", applicationId);
        
        // Check if there's an existing result and delete it
        loanApplicationResultRepository.findByLoanApplicationId(applicationId)
                .ifPresent(loanApplicationResultRepository::delete);
        
        // Find the loan application
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with ID: " + applicationId));
        
        // Create a new result with default values
        LoanApplicationResult result = new LoanApplicationResult();
        result.setLoanApplication(application);
        result.setStatus("APPROVED");
        result.setMessage("Congratulations! Your loan application has been approved.");
        result.setEligibilityScore(75);
        result.setMaxEligibleAmount(new BigDecimal("25000.00"));
        result.setSuggestedInterestRate("5.99%");
        result.setSuggestedTerm(36);
        result.setEstimatedMonthlyPayment(new BigDecimal("758.28"));
        
        // Save the result
        LoanApplicationResult savedResult = loanApplicationResultRepository.save(result);
        
        // Create some decision factors
        Map<String, Object> creditData = new HashMap<>();
        creditData.put("creditScore", 700);
        creditData.put("creditScoreRating", "Good");
        creditData.put("dti", 0.35);
        creditData.put("employmentStability", "Stable");
        creditData.put("paymentHistoryRating", "Excellent");
        
        decisionFactorService.createDecisionFactors(savedResult, creditData);
        
        // Return the DTO version
        return getLoanApplicationResult(applicationId);
    }
}
