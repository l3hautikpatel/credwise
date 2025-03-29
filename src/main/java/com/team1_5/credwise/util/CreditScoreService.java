package com.team1_5.credwise.util;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.math.BigDecimal;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CreditScoreService {
    private static final Logger logger = LoggerFactory.getLogger(CreditScoreService.class);

    /**
     * Calculate credit score based on financial data
     * @param creditData Map of credit-related data from loan application
     * @return The calculated credit score as a double
     */
    public double calculateCreditScore(Map<String, Object> creditData) {
        try {
            // Map data to the format expected by CanadianCreditScoringSystem
            Map<String, Object> profileData = prepareProfileData(creditData);
            
            // Calculate credit score using CanadianCreditScoringSystem
            int creditScore = CanadianCreditScoringSystem.calculateCreditScore(profileData);
            
            // Log the evaluation report for debugging
            String evaluationReport = CanadianCreditScoringSystem.evaluateCreditProfile(profileData);
            logger.info("Credit Evaluation Report: {}", evaluationReport);
            
            return creditScore;
        } catch (Exception e) {
            logger.error("Error calculating credit score: {}", e.getMessage(), e);
            // Return a default "review needed" score if calculation fails
            return 650;
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
        double income = getDoubleValue(creditData, "monthlyIncome", 0.0);
        double expenses = getDoubleValue(creditData, "monthlyExpenses", 0.0); 
        double debt = getDoubleValue(creditData, "estimatedDebts", 0.0);
        double loanRequest = getDoubleValue(creditData, "requestedAmount", 0.0);
        int tenure = getIntValue(creditData, "requestedTermMonths", 12);
        String paymentHistory = getStringValue(creditData, "paymentHistory", "On-time");
        double usedCredit = getDoubleValue(creditData, "creditTotalUsage", 0.0);
        double creditLimit = getDoubleValue(creditData, "currentCreditLimit", 1.0); // Avoid division by zero
        
        // For employment, sum up all months if there are multiple employments
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