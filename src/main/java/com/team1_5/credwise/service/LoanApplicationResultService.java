package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.LoanApplicationResult;
import com.team1_5.credwise.model.FinancialInfo;
import com.team1_5.credwise.repository.LoanApplicationRepository;
import com.team1_5.credwise.repository.LoanApplicationResultRepository;
import com.team1_5.credwise.repository.FinancialInfoRepository;
import com.team1_5.credwise.util.LoanApplicationResultMapper;
import com.team1_5.credwise.util.CanadianCreditScoringSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

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
        logger.info("Generating loan application result for application ID: {}", loanApplicationId);
        
        // Find the loan application
        LoanApplication application = loanApplicationRepository.findById(loanApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan Application not found with ID: " + loanApplicationId));
        
        // Check if the application has a result already
        loanApplicationResultRepository.findByLoanApplicationId(loanApplicationId)
                .ifPresent(existingResult -> {
                    logger.info("Deleting existing result for application ID: {}", loanApplicationId);
                    // Delete existing result
                    loanApplicationResultRepository.delete(existingResult);
                });
        
        // Get financial info to access eligibility score and system credit score
        FinancialInfo financialInfo = financialInfoRepository.findByLoanApplicationId(loanApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial Info not found for application: " + loanApplicationId));
        
        // Get credit evaluation data
        Map<String, Object> creditEvaluation = application.getCreditEvaluationData();
        if (creditEvaluation == null || creditEvaluation.isEmpty()) {
            logger.error("Credit evaluation data not available for loan application: {}", loanApplicationId);
            throw new IllegalStateException("Credit evaluation data not available for loan application: " + loanApplicationId);
        }
        
        logger.info("Credit evaluation data for application {}: {}", loanApplicationId, creditEvaluation);
        
        // Extract necessary values from application and financial info
        String decision = application.getStatus();
        Integer eligibilityScore = financialInfo.getEligibilityScore();
        Double approvalProbability = null;
        Double approvedAmount = null;
        Double interestRate = null;
        
        // Check if ML-based decision data is available
        if (creditEvaluation.containsKey("approval_probability")) {
            approvalProbability = ((Number) creditEvaluation.get("approval_probability")).doubleValue();
            logger.info("Approval probability from ML for application {}: {}", loanApplicationId, approvalProbability);
        }
        
        if (creditEvaluation.containsKey("approved_amount")) {
            approvedAmount = ((Number) creditEvaluation.get("approved_amount")).doubleValue();
            logger.info("Approved amount from ML for application {}: {}", loanApplicationId, approvedAmount);
        }
        
        if (creditEvaluation.containsKey("interest_rate")) {
            interestRate = ((Number) creditEvaluation.get("interest_rate")).doubleValue();
            logger.info("Interest rate from ML for application {}: {}", loanApplicationId, interestRate);
        }
        
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
        
        // Set eligibility score from financial info if available, otherwise calculate
        if (eligibilityScore != null) {
            result.setEligibilityScore(eligibilityScore);
        } else if (creditEvaluation.containsKey("eligibilityScore")) {
            result.setEligibilityScore(((Number) creditEvaluation.get("eligibilityScore")).intValue());
        } else {
            // Default calculation if no eligibility score is available
            Integer creditScore = financialInfo.getSystemCreditScore();
            if (creditScore == null) {
                creditScore = 650; // Default value if no system credit score
            }
            
            double dti = 0.4; // Default DTI
            if (financialInfo.getDebtToIncomeRatio() != null) {
                dti = financialInfo.getDebtToIncomeRatio().doubleValue();
            }
            
            String paymentHistory = "On-time"; // Default payment history
            int monthsEmployed = 0;
            if (financialInfo.getEmploymentDetails() != null && !financialInfo.getEmploymentDetails().isEmpty()) {
                monthsEmployed = financialInfo.getEmploymentDetails().stream()
                        .mapToInt(employment -> employment.getDurationMonths())
                        .sum();
            }
            
            int calculatedEligibilityScore = CanadianCreditScoringSystem.eligibilityScore(
                    creditScore, dti, paymentHistory, monthsEmployed);
            
            result.setEligibilityScore(calculatedEligibilityScore);
            
            // Update financial info with the calculated eligibility score
            if (financialInfo.getEligibilityScore() == null) {
                financialInfo.setEligibilityScore(calculatedEligibilityScore);
                financialInfoRepository.save(financialInfo);
                logger.info("Updated financial info with eligibility score {} for application {}", 
                        calculatedEligibilityScore, loanApplicationId);
            }
        }
        
        // Get original loan request details
        BigDecimal requestedAmount = application.getRequestedAmount();
        Integer requestedTerm = application.getRequestedTermMonths();
        
        // Calculate suggested terms based on credit score and ML data if available
        BigDecimal maxEligibleAmount;
        String suggestedInterestRate;
        Integer suggestedTerm;
        
        if (decision.equals("APPROVED")) {
            // If ML approved amount is available, use it
            if (approvedAmount != null) {
                maxEligibleAmount = BigDecimal.valueOf(approvedAmount).setScale(2, RoundingMode.HALF_UP);
            } else {
                // Approved for full amount
                maxEligibleAmount = requestedAmount;
            }
            
            // If ML interest rate is available, use it
            if (interestRate != null) {
                suggestedInterestRate = String.format("%.2f%%", interestRate);
            } else {
                // Calculate interest rate based on credit score
                Integer creditScore = financialInfo.getSystemCreditScore();
                if (creditScore == null) {
                    creditScore = 650; // Default value if no system credit score
                }
                
                double baseRate = CanadianCreditScoringSystem.getBaseInterestRate(application.getProductType());
                double adjustedRate = CanadianCreditScoringSystem.adjustInterestRate(baseRate, creditScore, requestedTerm);
                suggestedInterestRate = String.format("%.2f%%", adjustedRate * 100);
            }
            
            // Use requested term
            suggestedTerm = requestedTerm;
        } else {
            // Denied or under review
            Integer creditScore = financialInfo.getSystemCreditScore();
            if (creditScore == null) {
                creditScore = 600; // Default value if no system credit score
            }
            
            if (creditScore >= 600) {
                // Potentially eligible for a reduced amount
                if (approvedAmount != null) {
                    maxEligibleAmount = BigDecimal.valueOf(approvedAmount).setScale(2, RoundingMode.HALF_UP);
                } else {
                    maxEligibleAmount = requestedAmount.multiply(BigDecimal.valueOf(0.7))
                            .setScale(2, RoundingMode.HALF_UP);
                }
                
                // Interest rate
                if (interestRate != null) {
                    suggestedInterestRate = String.format("%.2f%%", interestRate);
                } else {
                    // Higher interest rate due to risk
                    double baseRate = CanadianCreditScoringSystem.getBaseInterestRate(application.getProductType());
                    double riskAdjustedRate = baseRate + 0.03; // 3% higher
                    suggestedInterestRate = String.format("%.2f%%", riskAdjustedRate * 100);
                }
                
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
            
            double emi = CanadianCreditScoringSystem.calculateEMI(principal, rate / 12, suggestedTerm);
            result.setEstimatedMonthlyPayment(BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP));
        } else {
            result.setEstimatedMonthlyPayment(BigDecimal.ZERO);
        }
        
        // Save the result
        logger.info("Saving loan application result for application {}", loanApplicationId);
        LoanApplicationResult savedResult = loanApplicationResultRepository.save(result);
        
        // Create decision factors
        logger.info("Creating decision factors for application {}", loanApplicationId);
        decisionFactorService.createDecisionFactors(savedResult, creditEvaluation);
        
        return savedResult;
    }
}
