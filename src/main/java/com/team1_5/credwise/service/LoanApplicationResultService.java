package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.LoanApplicationResult;
import com.team1_5.credwise.repository.LoanApplicationRepository;
import com.team1_5.credwise.repository.LoanApplicationResultRepository;
import com.team1_5.credwise.util.LoanApplicationResultMapper;
import com.team1_5.credwise.util.CanadianCreditScoringSystem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class LoanApplicationResultService {
    private final LoanApplicationResultRepository loanApplicationResultRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final DecisionFactorService decisionFactorService;

    public LoanApplicationResultService(
            LoanApplicationResultRepository loanApplicationResultRepository, 
            LoanApplicationRepository loanApplicationRepository,
            DecisionFactorService decisionFactorService) {
        this.loanApplicationResultRepository = loanApplicationResultRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.decisionFactorService = decisionFactorService;
    }

    @Transactional(readOnly = true)
    public LoanApplicationResultResponse getLoanApplicationResult(Long loanApplicationId) {
        LoanApplicationResult result = loanApplicationResultRepository.findByLoanApplicationId(loanApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan Application Result not found for application ID: " + loanApplicationId));

        return new LoanApplicationResultResponse(
                result.getStatus(),
                result.getMessage(),
                result.getEligibilityScore(),
                result.getMaxEligibleAmount(),
                result.getSuggestedInterestRate(),
                result.getSuggestedTerm(),
                result.getEstimatedMonthlyPayment(),
                result.getDecisionFactors().stream()
                        .map(factor -> new LoanApplicationResultResponse.DecisionFactorResponse(factor.getFactor(), factor.getImpact(), factor.getDescription()))
                        .toList()
        );
    }
    
    /**
     * Generate a loan application result from a loan application using credit evaluation data
     * @param loanApplicationId The ID of the loan application
     * @return The generated loan application result
     */
    @Transactional
    public LoanApplicationResult generateLoanApplicationResult(Long loanApplicationId) {
        // Find the loan application
        LoanApplication application = loanApplicationRepository.findById(loanApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan Application not found with ID: " + loanApplicationId));
        
        // Check if the application has a result already
        loanApplicationResultRepository.findByLoanApplicationId(loanApplicationId)
                .ifPresent(existingResult -> {
                    // Delete existing result
                    loanApplicationResultRepository.delete(existingResult);
                });
        
        // Get credit evaluation data
        Map<String, Object> creditEvaluation = application.getCreditEvaluationData();
        if (creditEvaluation == null || creditEvaluation.isEmpty()) {
            throw new IllegalStateException("Credit evaluation data not available for loan application: " + loanApplicationId);
        }
        
        // Extract necessary values from credit evaluation
        int creditScore = ((Number) creditEvaluation.get("creditScore")).intValue();
        String decision = application.getStatus();
        
        // Create the result
        LoanApplicationResult result = new LoanApplicationResult();
        result.setLoanApplication(application);
        result.setStatus(decision);
        
        // Set message based on status
        if (decision.equals("APPROVED")) {
            result.setMessage("Your loan application has been approved!");
        } else if (decision.equals("DENIED")) {
            result.setMessage("We're sorry, your loan application has been denied.");
        } else {
            result.setMessage("Your loan application is pending review.");
        }
        
        // Calculate eligibility score using CanadianCreditScoringSystem if available
        int eligibilityScore = 0;
        if (creditEvaluation.containsKey("dti")) {
            double dti = ((Number) creditEvaluation.get("dti")).doubleValue();
            String paymentHistory = (String) creditEvaluation.getOrDefault("paymentHistory", "On-time");
            int monthsEmployed = ((Number) creditEvaluation.getOrDefault("monthsEmployed", 0)).intValue();
            
            eligibilityScore = CanadianCreditScoringSystem.eligibilityScore(
                    creditScore, dti, paymentHistory, monthsEmployed);
        } else {
            // Default calculation if detailed data is not available
            eligibilityScore = Math.max(0, Math.min(100, (creditScore - 300) / 6));
        }
        result.setEligibilityScore(eligibilityScore);
        
        // Get original loan request details
        BigDecimal requestedAmount = application.getRequestedAmount();
        Integer requestedTerm = application.getRequestedTermMonths();
        
        // Calculate suggested terms based on credit score
        BigDecimal maxEligibleAmount;
        String suggestedInterestRate;
        Integer suggestedTerm;
        
        if (decision.equals("APPROVED")) {
            // Approved for full amount
            maxEligibleAmount = requestedAmount;
            
            // Calculate interest rate based on credit score
            double baseRate = CanadianCreditScoringSystem.getBaseInterestRate(application.getProductType());
            double adjustedRate = CanadianCreditScoringSystem.adjustInterestRate(baseRate, creditScore, requestedTerm);
            suggestedInterestRate = String.format("%.2f%%", adjustedRate * 100);
            
            // Use requested term
            suggestedTerm = requestedTerm;
        } else {
            // Denied or under review
            if (creditScore >= 600) {
                // Potentially eligible for a reduced amount
                maxEligibleAmount = requestedAmount.multiply(BigDecimal.valueOf(0.7))
                        .setScale(2, RoundingMode.HALF_UP);
                
                // Higher interest rate due to risk
                double baseRate = CanadianCreditScoringSystem.getBaseInterestRate(application.getProductType());
                double riskAdjustedRate = baseRate + 0.03; // 3% higher
                suggestedInterestRate = String.format("%.2f%%", riskAdjustedRate * 100);
                
                // Suggest shorter term
                suggestedTerm = Math.min(requestedTerm, 36); // Max 36 months for higher risk
            } else {
                // Not eligible
                maxEligibleAmount = BigDecimal.ZERO;
                suggestedInterestRate = "N/A";
                suggestedTerm = 0;
            }
        }
        result.setMaxEligibleAmount(maxEligibleAmount);
        result.setSuggestedInterestRate(suggestedInterestRate);
        result.setSuggestedTerm(suggestedTerm);
        
        // Calculate estimated monthly payment
        if (maxEligibleAmount.compareTo(BigDecimal.ZERO) > 0 && suggestedTerm > 0 && !suggestedInterestRate.equals("N/A")) {
            double principal = maxEligibleAmount.doubleValue();
            double rate = Double.parseDouble(suggestedInterestRate.replace("%", "")) / 100;
            
            double emi = CanadianCreditScoringSystem.calculateEMI(principal, rate, suggestedTerm);
            result.setEstimatedMonthlyPayment(BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP));
        } else {
            result.setEstimatedMonthlyPayment(BigDecimal.ZERO);
        }
        
        // Save the result
        LoanApplicationResult savedResult = loanApplicationResultRepository.save(result);
        
        // Create decision factors
        decisionFactorService.createDecisionFactors(savedResult, creditEvaluation);
        
        return savedResult;
    }
}
