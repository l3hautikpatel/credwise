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
import java.util.List;
import java.util.ArrayList;

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
            System.out.println("Calculating credit score with data: " + creditData);
            
            // Map data to the format expected by CanadianCreditScoringSystem
            Map<String, Object> profileData = prepareProfileData(creditData);
            
            // Calculate credit score using CanadianCreditScoringSystem
            int creditScore = CanadianCreditScoringSystem.calculateCreditScore(profileData);
            
            // Log the evaluation report for debugging
            String evaluationReport = CanadianCreditScoringSystem.evaluateCreditProfile(profileData);
            System.out.println("Credit Evaluation Report: " + evaluationReport);
            
            // Store system-generated credit score in financial info
            if (financialInfo != null) {
                try {
                    System.out.println("Setting system credit score to " + creditScore + 
                                    " for financial info ID: " + financialInfo.getId());
                    
                    financialInfo.setSystemCreditScore(creditScore);
                    
                    // Calculate and store eligibility score
                    if (profileData.containsKey("income") && profileData.containsKey("expenses") && 
                        profileData.containsKey("debt") && profileData.containsKey("loanRequest")) {
                        
                        double dti = calculateDTI(profileData);
                        String paymentHistory = (String) profileData.get("paymentHistory");
                        int monthsEmployed = getIntValue(profileData, "monthsEmployed", 0);
                        
                        int eligibilityScore = CanadianCreditScoringSystem.eligibilityScore(
                            creditScore, dti, paymentHistory, monthsEmployed
                        );
                        
                        System.out.println("Setting eligibility score to " + eligibilityScore + 
                                        " for financial info ID: " + financialInfo.getId());
                        
                        financialInfo.setEligibilityScore(eligibilityScore);
                        System.out.println("Updated financial info with scores - Credit: " + creditScore + 
                                        ", Eligibility: " + eligibilityScore);
                    } else {
                        System.out.println("Missing data for eligibility score calculation");
                    }
                } catch (Exception e) {
                    System.out.println("Error updating financial info with credit scores: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("FinancialInfo is null, not updating");
            }
            
            // Extract key metrics for decision factors
            double dti = calculateDTI(profileData);
            
            String employmentStability = CanadianCreditScoringSystem.determineEmploymentStability(
                getString(profileData, "employmentStatus", "Unemployed"),
                getIntValue(profileData, "monthsEmployed", 0)
            );
            
            String paymentHistoryRating = CanadianCreditScoringSystem.determinePaymentHistoryRating(
                getString(profileData, "paymentHistory", "")
            );
            
            // Prepare result with credit score and decision factors
            result.put("creditScore", creditScore);
            result.put("creditScoreRating", CanadianCreditScoringSystem.creditScoreRating(creditScore));
            result.put("dti", dti);
            result.put("dtiRating", dti < 0.4 ? "Positive" : "Negative");
            result.put("employmentStability", employmentStability);
            result.put("paymentHistoryRating", paymentHistoryRating);
            
            // Calculate and include eligibility score
            if (profileData.containsKey("paymentHistory") && profileData.containsKey("monthsEmployed")) {
                String paymentHistory = getString(profileData, "paymentHistory", "");
                int monthsEmployed = getIntValue(profileData, "monthsEmployed", 0);
                
                int eligibilityScore = CanadianCreditScoringSystem.eligibilityScore(
                    creditScore, dti, paymentHistory, monthsEmployed
                );
                
                result.put("eligibilityScore", eligibilityScore);
            }
            
            // Calculate credit utilization percentage if available
            if (profileData.containsKey("creditLimit") && profileData.containsKey("usedCredit")) {
                double usedCredit = getDoubleValue(profileData, "usedCredit", 0.0);
                double creditLimit = getDoubleValue(profileData, "creditLimit", 1.0);
                
                if (creditLimit > 0) {
                    double utilizationPercent = (usedCredit / creditLimit) * 100;
                    result.put("creditUtilization", utilizationPercent);
                    result.put("creditUtilizationRating", 
                        utilizationPercent < 30 ? "Good" : 
                        utilizationPercent < 50 ? "Fair" : 
                        utilizationPercent < 75 ? "High" : "Very High");
                }
            }
            
            System.out.println("Credit score calculation completed successfully: " + result);
            return result;
            
        } catch (Exception e) {
            System.out.println("Error calculating credit score: " + e.getMessage());
            e.printStackTrace();
            
            // Return error information instead of default score
            result.put("error", "Error calculating credit score: " + e.getMessage());
            result.put("creditScore", 650); // Default score on error
            result.put("eligibilityScore", 50); // Default eligibility on error
            
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
        // Print what we received to help debug
        System.out.println("prepareProfileData received: " + creditData);
        
        // Extract and convert values with appropriate defaults as needed
        String loanType = getStringValue(creditData, "loanType", "Personal Loan");
        
        // Income and expense data - check various possible field names
        double income = getDoubleValue(creditData, "income", 0.0);
        if (income == 0.0) {
            income = getDoubleValue(creditData, "monthlyIncome", 0.0);
        }
        
        double expenses = getDoubleValue(creditData, "expenses", 0.0);
        if (expenses == 0.0) {
            expenses = getDoubleValue(creditData, "monthlyExpenses", 0.0);
        }
        
        double debt = getDoubleValue(creditData, "debt", 0.0);
        if (debt == 0.0) {
            debt = getDoubleValue(creditData, "estimatedDebts", 0.0);
        }
        
        double loanRequest = getDoubleValue(creditData, "loanRequest", 0.0);
        if (loanRequest == 0.0) {
            loanRequest = getDoubleValue(creditData, "requestedAmount", 0.0);
        }
        
        int tenure = getIntValue(creditData, "tenure", 12);
        if (tenure == 12) { // Only default
            tenure = getIntValue(creditData, "requestedTermMonths", 12);
        }
        
        // Try both keys for payment history
        String paymentHistory = getStringValue(creditData, "payment_history", null);
        if (paymentHistory == null) {
            paymentHistory = getStringValue(creditData, "paymentHistory", "On-time");
        }
        
        // Try both keys for credit usage
        double usedCredit = getDoubleValue(creditData, "used_credit", 0.0);
        if (usedCredit == 0.0) {
            usedCredit = getDoubleValue(creditData, "creditTotalUsage", 0.0);
        }
        
        // Try both keys for credit limit
        double creditLimit = getDoubleValue(creditData, "credit_limit", 1000.0); // Avoid division by zero
        if (creditLimit == 1000.0 && creditData.containsKey("currentCreditLimit")) {
            creditLimit = getDoubleValue(creditData, "currentCreditLimit", 1000.0);
        }
        
        // For employment - check multiple possible keys
        String employmentStatus = getStringValue(creditData, "employmentStatus", null);
        if (employmentStatus == null) {
            employmentStatus = getStringValue(creditData, "employmentType", "Full-time");
        }
        
        int monthsEmployed = getIntValue(creditData, "monthsEmployed", 0);
        if (monthsEmployed == 0 && creditData.containsKey("employmentDurationMonths")) {
            monthsEmployed = getIntValue(creditData, "employmentDurationMonths", 0);
        }
        
        // For assets/bank accounts
        double assets = getDoubleValue(creditData, "totalAssets", 0.0);
        int bankAccounts = getIntValue(creditData, "bankAccounts", 1);
        
        // Create debt types set from either key (default to empty set if not available)
        Set<String> debtTypes = new HashSet<>();
        try {
            if (creditData.containsKey("debt_types")) {
                Object debtTypesObj = creditData.get("debt_types");
                if (debtTypesObj instanceof Set) {
                    debtTypes = (Set<String>) debtTypesObj;
                }
            } else if (creditData.containsKey("debtTypes")) {
                Object debtTypesObj = creditData.get("debtTypes");
                if (debtTypesObj instanceof Set) {
                    debtTypes = (Set<String>) debtTypesObj;
                }
            }
        } catch (Exception e) {
            System.out.println("Error processing debt types: " + e.getMessage());
        }
        
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
        
        // Print the prepared data for debugging
        System.out.println("prepareProfileData output: " + profileData);
        
        return profileData;
    }
    
    // Helper methods for safe value extraction
    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        if (map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (Exception e) {
                // Fallback to default
            }
        }
        return defaultValue;
    }
    
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (Exception e) {
                // Fallback to default
            }
        }
        return defaultValue;
    }

    // Helper method to calculate DTI
    private double calculateDTI(Map<String, Object> profileData) {
        if (!profileData.containsKey("income") || getDoubleValue(profileData, "income", 0.0) <= 0) {
            return 1.0; // Default to max DTI if no income
        }
        
        double income = getDoubleValue(profileData, "income", 0.0);
        double expenses = getDoubleValue(profileData, "expenses", 0.0);
        double debt = getDoubleValue(profileData, "debt", 0.0);
        double loanRequest = getDoubleValue(profileData, "loanRequest", 0.0);
        
        return CanadianCreditScoringSystem.dtiScore(income, expenses, debt, loanRequest);
    }

    // Safe getter methods
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map.containsKey(key) && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return defaultValue;
    }
}