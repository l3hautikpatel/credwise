package com.team1_5.credwise.util;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.math.BigDecimal;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.team1_5.credwise.model.FinancialInfo;
import java.util.HashMap;

@Service
public class CreditScoreService {
    private static final Logger logger = LoggerFactory.getLogger(CreditScoreService.class);

    /**
     * Calculate credit score based on financial data and update the financial info
     * @param creditData Map of credit-related data from loan application
     * @param financialInfo The FinancialInfo entity to update with system-generated score
     * @return Map containing the calculated credit score and decision factors
     */
    public Map<String, Object> calculateCreditScore(Map<String, Object> creditData, FinancialInfo financialInfo) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Map data to the format expected by CanadianCreditScoringSystem
            Map<String, Object> profileData = prepareProfileData(creditData);
            
            // Calculate credit score using CanadianCreditScoringSystem
            int creditScore = CanadianCreditScoringSystem.calculateCreditScore(profileData);
            
            // Log the evaluation report for debugging
            String evaluationReport = CanadianCreditScoringSystem.evaluateCreditProfile(profileData);
            logger.info("Credit Evaluation Report: {}", evaluationReport);
            
            // Store system-generated credit score in financial info
            if (financialInfo != null) {
                financialInfo.setSystemCreditScore(creditScore);
                
                // Calculate and store eligibility score
                double dti = CanadianCreditScoringSystem.dtiScore(
                    (double) profileData.get("income"),
                    (double) profileData.get("expenses"),
                    (double) profileData.get("debt"),
                    (double) profileData.get("loanRequest")
                );
                
                String paymentHistory = (String) profileData.get("paymentHistory");
                int monthsEmployed = (int) profileData.get("monthsEmployed");
                
                int eligibilityScore = CanadianCreditScoringSystem.eligibilityScore(
                    creditScore, dti, paymentHistory, monthsEmployed
                );
                
                financialInfo.setEligibilityScore(eligibilityScore);
            }
            
            // Extract key metrics for decision factors
            double dti = CanadianCreditScoringSystem.dtiScore(
                (double) profileData.get("income"),
                (double) profileData.get("expenses"),
                (double) profileData.get("debt"),
                (double) profileData.get("loanRequest")
            );
            
            String employmentStability = CanadianCreditScoringSystem.determineEmploymentStability(
                (String) profileData.get("employmentStatus"),
                (int) profileData.get("monthsEmployed")
            );
            
            String paymentHistoryRating = CanadianCreditScoringSystem.determinePaymentHistoryRating(
                (String) profileData.get("paymentHistory")
            );
            
            // Prepare result with credit score and decision factors
            result.put("creditScore", creditScore);
            result.put("creditScoreRating", CanadianCreditScoringSystem.creditScoreRating(creditScore));
            result.put("dti", dti);
            result.put("dtiRating", dti < 0.4 ? "Positive" : "Negative");
            result.put("employmentStability", employmentStability);
            result.put("paymentHistoryRating", paymentHistoryRating);
            
            // Calculate and include eligibility score
            if (!result.containsKey("eligibilityScore") && profileData.containsKey("paymentHistory") && profileData.containsKey("monthsEmployed")) {
                String paymentHistory = (String) profileData.get("paymentHistory");
                int monthsEmployed = (int) profileData.get("monthsEmployed");
                
                int eligibilityScore = CanadianCreditScoringSystem.eligibilityScore(
                    creditScore, dti, paymentHistory, monthsEmployed
                );
                
                result.put("eligibilityScore", eligibilityScore);
            }
            
            // Check credit score difference if user provided one
            Integer userCreditScore = financialInfo != null ? financialInfo.getCreditScore() : null;
            if (userCreditScore != null) {
                int difference = Math.abs(userCreditScore - creditScore);
                boolean isScoreAccurate = difference <= 25;
                result.put("creditScoreDifference", difference);
                result.put("isScoreAccurate", isScoreAccurate);
                result.put("creditScoreAccuracyMessage", generateCreditScoreAccuracyMessage(userCreditScore, creditScore, isScoreAccurate));
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error calculating credit score: {}", e.getMessage(), e);
            
            // Return error information instead of default score
            result.put("error", "Error calculating credit score: " + e.getMessage());
            result.put("creditScore", null);
            
            return result;
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public double calculateCreditScore(Map<String, Object> creditData) {
        Map<String, Object> result = calculateCreditScore(creditData, null);
        Number creditScore = (Number) result.get("creditScore");
        if (creditScore == null) {
            return 0; // Return 0 instead of default 650
        }
        return creditScore.doubleValue();
    }
    
    /**
     * Generate message about credit score accuracy
     */
    private String generateCreditScoreAccuracyMessage(int userCreditScore, int systemCreditScore, boolean isAccurate) {
        if (isAccurate) {
            return "Reported credit score matches our calculations.";
        } else {
            return "The reported credit score differs significantly from our calculations.";
        }
    }
    
    /**
     * Prepare credit profile data for the CanadianCreditScoringSystem
     * @param creditData Raw credit data from loan application
     * @return Mapped data in the format expected by CanadianCreditScoringSystem
     */
    private Map<String, Object> prepareProfileData(Map<String, Object> creditData) {
        // The data from prepareCreditData in LoanApplicationService needs to be mapped
        // to the format expected by CanadianCreditScoringSystem
        
        // Extract and convert values with appropriate defaults as needed
        String loanType = getStringValue(creditData, "loanType", "Personal Loan");
        
        // Get income as monthly and convert to yearly if needed
        double monthlyIncome = getDoubleValue(creditData, "monthlyIncome", 0.0);
        double income = monthlyIncome; // Keep as monthly for CanadianCreditScoringSystem
        
        double expenses = getDoubleValue(creditData, "monthlyExpenses", 0.0); 
        double debt = getDoubleValue(creditData, "estimatedDebts", 0.0);
        double loanRequest = getDoubleValue(creditData, "requestedAmount", 0.0);
        int tenure = getIntValue(creditData, "requestedTermMonths", 12);
        String paymentHistory = getStringValue(creditData, "paymentHistory", "On-time");
        double usedCredit = getDoubleValue(creditData, "creditTotalUsage", 0.0);
        double creditLimit = getDoubleValue(creditData, "currentCreditLimit", 1.0); // Avoid division by zero
        
        // For employment
        String employmentStatus = getStringValue(creditData, "employmentType", "Full-time");
        int monthsEmployed = getIntValue(creditData, "employmentDurationMonths", 0);
        
        // For assets/bank accounts
        double assets = getDoubleValue(creditData, "totalAssets", 0.0);
        Integer bankAccounts = (Integer) creditData.getOrDefault("bankAccounts", 1);
        
        // Create debt types set (default to empty set if not available)
        @SuppressWarnings("unchecked")
        Set<String> debtTypes = (Set<String>) creditData.getOrDefault("debtTypes", new HashSet<>());
        
        // Create and populate the profile data map
        Map<String, Object> profileData = new java.util.HashMap<>();
        profileData.put("loanType", loanType);
        profileData.put("income", income);
        profileData.put("expenses", expenses);
        profileData.put("debt", debt);
        profileData.put("loanRequest", loanRequest);
        profileData.put("tenure", tenure);
        profileData.put("paymentHistory", paymentHistory);
        profileData.put("usedCredit", usedCredit);
        profileData.put("creditLimit", creditLimit);
        profileData.put("employmentStatus", employmentStatus);
        profileData.put("monthsEmployed", monthsEmployed);
        profileData.put("assets", assets);
        profileData.put("bankAccounts", bankAccounts);
        profileData.put("debtTypes", debtTypes);
        
        return profileData;
    }
    
    // Helper methods for safe value extraction
    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    private double getDoubleValue(Map<String, Object> data, String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        return defaultValue;
    }
    
    private int getIntValue(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}