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
        
        // Get the credit evaluation data
        Map<String, Object> creditData = application.getCreditEvaluationData();
        if (creditData == null) {
            throw new ResourceNotFoundException("Credit evaluation data not found for application ID: " + applicationId);
        }
        
        logger.info("Credit evaluation data for application {}: {}", applicationId, creditData);

        // Create a new result with calculated values
        LoanApplicationResult result = new LoanApplicationResult();
        result.setLoanApplication(application);
        
        // Set status and message based on the application status
        // Honor the application status which should have been set by ML or fallback
        String status = application.getStatus();
        boolean isApproved = "APPROVED".equals(status);
        boolean isDenied = "DENIED".equals(status);
        boolean needsReview = "REVIEW_NEEDED".equals(status);
        
        logger.info("Application {} has status: {}", applicationId, status);
        
        if (needsReview) {
            result.setStatus("REVIEW_NEEDED");
            result.setMessage("Your application requires additional review by our team.");
        } else if (isDenied) {
            result.setStatus("DENIED");
            result.setMessage("We regret to inform you that your loan application has been denied.");
        } else if (isApproved) {
            result.setStatus("APPROVED");
            result.setMessage("Congratulations! Your loan application has been approved.");
        } else {
            // Default to SUBMITTED if none of the above
            result.setStatus("SUBMITTED");
            result.setMessage("Your application has been submitted and is being processed.");
        }
        
        // Get credit score from various possible sources
        Integer creditScore = null;
        if (creditData.containsKey("creditScore")) {
            creditScore = getIntValue(creditData, "creditScore", 0);
            logger.info("Using creditScore from credit data: {}", creditScore);
        } else if (creditData.containsKey("credit_score")) {
            creditScore = getIntValue(creditData, "credit_score", 0);
            logger.info("Using credit_score from credit data: {}", creditScore);
        } else if (creditData.containsKey("predicted_credit_score")) {
            creditScore = getIntValue(creditData, "predicted_credit_score", 0);
            logger.info("Using predicted_credit_score from credit data: {}", creditScore);
        } else if (application.getCreditScore() != null) {
            creditScore = application.getCreditScore().intValue();
            logger.info("Using credit score from application: {}", creditScore);
        } else {
            // Default if no credit score is available
            creditScore = 650;
            logger.warn("No credit score found, using default: {}", creditScore);
        }
        
        // Set eligibility score equal to credit score if not explicitly provided
        Integer eligibilityScore = creditScore;
        if (creditData.containsKey("eligibilityScore")) {
            eligibilityScore = getIntValue(creditData, "eligibilityScore", creditScore);
            logger.info("Using eligibility score from credit data: {}", eligibilityScore);
        }
        
        result.setEligibilityScore(eligibilityScore);
        
        // Calculate max eligible amount based on approval status and credit score
        BigDecimal maxEligibleAmount;
        if (isDenied) {
            // If denied, set to zero
            maxEligibleAmount = BigDecimal.ZERO;
        } else if (creditData.containsKey("approved_amount")) {
            // Use ML-provided amount if available
            maxEligibleAmount = new BigDecimal(creditData.get("approved_amount").toString());
            logger.info("Using approved_amount from ML: {}", maxEligibleAmount);
        } else if (creditData.containsKey("max_eligible_amount")) {
            // Alternative field name
            maxEligibleAmount = new BigDecimal(creditData.get("max_eligible_amount").toString());
            logger.info("Using max_eligible_amount from credit data: {}", maxEligibleAmount);
        } else {
            // Calculate based on credit score if not provided by ML service
            maxEligibleAmount = calculateMaxEligibleAmount(creditScore, application.getRequestedAmount());
            logger.info("Calculated max eligible amount: {}", maxEligibleAmount);
        }
        
        result.setMaxEligibleAmount(maxEligibleAmount);
        
        // Calculate interest rate based on approval status and credit score
        String interestRate;
        if (isDenied) {
            // If denied, use a placeholder rate
            interestRate = "N/A";
        } else if (creditData.containsKey("interest_rate")) {
            // Use ML-provided rate if available
            Object rateObj = creditData.get("interest_rate");
            if (rateObj instanceof String && ((String)rateObj).contains("%")) {
                interestRate = (String)rateObj;
            } else {
                double rate = getDoubleValue(creditData, "interest_rate", 0.05);
                interestRate = String.format("%.2f%%", rate );
            }
            logger.info("Using interest rate from ML: {}", interestRate);
        } else if (creditData.containsKey("interest_rate_formatted")) {
            interestRate = (String)creditData.get("interest_rate_formatted");
            logger.info("Using formatted interest rate from credit data: {}", interestRate);
        } else {
            // Calculate based on credit score
            interestRate = calculateInterestRate(creditScore);
            logger.info("Calculated interest rate: {}", interestRate);
        }
        
        result.setSuggestedInterestRate(interestRate);
        
        // Set suggested term based on application request
        result.setSuggestedTerm(application.getRequestedTermMonths());
        
        // Calculate monthly payment only if approved
        if (isApproved && !interestRate.equals("N/A")) {
            try {
                // Strip percentage sign if present
                String interestRateStr = interestRate.replace("%", "");
                BigDecimal rateValue = new BigDecimal(interestRateStr);
                BigDecimal monthlyPayment = calculateMonthlyPayment(
                    maxEligibleAmount,
                    rateValue,
                    application.getRequestedTermMonths()
                );
                result.setEstimatedMonthlyPayment(monthlyPayment);
                logger.info("Calculated monthly payment: {}", monthlyPayment);
            } catch (Exception e) {
                logger.error("Error calculating monthly payment: {}", e.getMessage());
                result.setEstimatedMonthlyPayment(BigDecimal.ZERO);
            }
        } else {
            result.setEstimatedMonthlyPayment(BigDecimal.ZERO);
        }
        
        // Save the result
        LoanApplicationResult savedResult = loanApplicationResultRepository.save(result);
        logger.info("Saved loan application result with ID: {}", savedResult.getId());
        
        // Create decision factors based on credit data
        try {
            decisionFactorService.createDecisionFactors(savedResult, creditData);
            logger.info("Created decision factors for result ID: {}", savedResult.getId());
        } catch (Exception e) {
            logger.error("Error creating decision factors: {}", e.getMessage());
        }
        
        // Return the DTO version
        return getLoanApplicationResult(applicationId);
    }

    private BigDecimal calculateMaxEligibleAmount(Integer creditScore, BigDecimal requestedAmount) {
        if (creditScore == null || creditScore < 600) {
            return BigDecimal.ZERO;
        } else if (creditScore < 650) {
            return requestedAmount.min(new BigDecimal("10000.00"));
        } else if (creditScore < 700) {
            return requestedAmount.min(new BigDecimal("25000.00"));
        } else if (creditScore < 750) {
            return requestedAmount.min(new BigDecimal("50000.00"));
        } else {
            return requestedAmount.min(new BigDecimal("100000.00"));
        }
    }

    private String calculateInterestRate(Integer creditScore) {
        if (creditScore == null || creditScore < 600) {
            return "15.99%";
        } else if (creditScore < 650) {
            return "12.99%";
        } else if (creditScore < 700) {
            return "9.99%";
        } else if (creditScore < 750) {
            return "6.99%";
        } else {
            return "4.99%";
        }
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, Integer termMonths) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 8, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = principal.multiply(monthlyRate)
            .multiply(BigDecimal.ONE.add(monthlyRate).pow(termMonths))
            .divide(BigDecimal.ONE.add(monthlyRate).pow(termMonths).subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        return monthlyPayment;
    }

    // Helper methods for safe type conversion
    private int getIntValue(Map<String, Object> data, String key, int defaultValue) {
        if (data.containsKey(key) && data.get(key) != null) {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (Exception e) {
                logger.warn("Failed to parse integer value for key '{}': {}", key, e.getMessage());
            }
        }
        return defaultValue;
    }
    
    private double getDoubleValue(Map<String, Object> data, String key, double defaultValue) {
        if (data.containsKey(key) && data.get(key) != null) {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (Exception e) {
                logger.warn("Failed to parse double value for key '{}': {}", key, e.getMessage());
            }
        }
        return defaultValue;
    }
}
