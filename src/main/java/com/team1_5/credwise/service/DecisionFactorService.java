package com.team1_5.credwise.service;

import com.team1_5.credwise.model.DecisionFactor;
import com.team1_5.credwise.model.LoanApplicationResult;
import com.team1_5.credwise.repository.DecisionFactorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DecisionFactorService {

    private final DecisionFactorRepository decisionFactorRepository;

    public DecisionFactorService(DecisionFactorRepository decisionFactorRepository) {
        this.decisionFactorRepository = decisionFactorRepository;
    }

    /**
     * Create decision factors for a loan application result based on credit evaluation
     * @param result The loan application result entity
     * @param creditEvaluationData Credit evaluation data from the scoring system
     */
    @Transactional
    public void createDecisionFactors(LoanApplicationResult result, Map<String, Object> creditEvaluationData) {
        // First delete any existing decision factors for this result
        decisionFactorRepository.deleteByResult(result);

        List<DecisionFactor> factors = new ArrayList<>();
        System.out.println("Creating decision factors with data: " + creditEvaluationData);

        try {
            // Credit Score factor - handle different possible field names
            int creditScore = 0;
            String creditScoreRating = "Fair";
            
            if (creditEvaluationData.containsKey("creditScore")) {
                creditScore = getIntValue(creditEvaluationData, "creditScore", 0);
            } else if (creditEvaluationData.containsKey("credit_score")) {
                creditScore = getIntValue(creditEvaluationData, "credit_score", 0);
            } else if (creditEvaluationData.containsKey("predicted_credit_score")) {
                creditScore = getIntValue(creditEvaluationData, "predicted_credit_score", 0);
            }
            
            if (creditEvaluationData.containsKey("creditScoreRating")) {
                creditScoreRating = (String) creditEvaluationData.get("creditScoreRating");
            } else {
                // Determine rating based on score
                if (creditScore >= 750) creditScoreRating = "Excellent";
                else if (creditScore >= 700) creditScoreRating = "Very Good";
                else if (creditScore >= 650) creditScoreRating = "Good";
                else if (creditScore >= 600) creditScoreRating = "Fair";
                else creditScoreRating = "Poor";
            }
            
            String creditScoreImpact = creditScore >= 600 ? "Positive" : "Negative";
            String creditScoreDescription = creditScore >= 600 
                    ? "Credit score of " + creditScore + " (" + creditScoreRating + ") is sufficient." 
                    : "Credit score of " + creditScore + " (" + creditScoreRating + ") is below recommended minimum.";
                    
            factors.add(createFactor(result, "Credit Score", creditScoreImpact, creditScoreDescription));
            System.out.println("Added Credit Score factor: " + creditScoreImpact + " - " + creditScoreDescription);
    
            // Debt-to-Income Ratio factor - handle different possible field names
            double dti = 0.0;
            if (creditEvaluationData.containsKey("dti")) {
                dti = getDoubleValue(creditEvaluationData, "dti", 0.0) / 100.0; // Convert percentage to decimal
            } else if (creditEvaluationData.containsKey("debt_to_income_ratio")) {
                dti = getDoubleValue(creditEvaluationData, "debt_to_income_ratio", 0.0);
            } else if (creditEvaluationData.containsKey("dti_ratio")) {
                dti = getDoubleValue(creditEvaluationData, "dti_ratio", 0.0);
            }
            
            String dtiImpact = dti < 0.4 ? "Positive" : "Negative";
            String dtiDescription = dti < 0.4 
                    ? "Debt-to-income ratio of " + String.format("%.1f%%", dti * 100) + " is within acceptable range." 
                    : "Debt-to-income ratio of " + String.format("%.1f%%", dti * 100) + " exceeds recommended maximum.";
                    
            factors.add(createFactor(result, "Debt-to-Income Ratio", dtiImpact, dtiDescription));
            System.out.println("Added DTI factor: " + dtiImpact + " - " + dtiDescription);
    
            // Employment Stability factor - handle different possible field names
            String employmentStability = "Unknown";
            
            if (creditEvaluationData.containsKey("employmentStability")) {
                employmentStability = (String) creditEvaluationData.get("employmentStability");
            } else if (creditEvaluationData.containsKey("employment_stability")) {
                employmentStability = (String) creditEvaluationData.get("employment_stability");
            } else {
                // Try to determine from employment status and duration
                String employmentStatus = "";
                int monthsEmployed = 0;
                
                if (creditEvaluationData.containsKey("employmentStatus")) {
                    employmentStatus = (String) creditEvaluationData.get("employmentStatus");
                } else if (creditEvaluationData.containsKey("employment_status")) {
                    employmentStatus = (String) creditEvaluationData.get("employment_status");
                }
                
                if (creditEvaluationData.containsKey("monthsEmployed")) {
                    monthsEmployed = getIntValue(creditEvaluationData, "monthsEmployed", 0);
                } else if (creditEvaluationData.containsKey("months_employed")) {
                    monthsEmployed = getIntValue(creditEvaluationData, "months_employed", 0);
                }
                
                // Determine stability based on employment status and duration
                boolean isFullTime = employmentStatus.toLowerCase().contains("full-time") || 
                                    employmentStatus.toLowerCase().contains("fulltime");
                boolean isStable = (isFullTime && monthsEmployed >= 12) || (monthsEmployed >= 24);
                employmentStability = isStable ? "Stable" : "Unstable";
            }
            
            String employmentImpact = employmentStability.equalsIgnoreCase("Stable") ? "Positive" : "Negative";
            String employmentDescription = employmentStability.equalsIgnoreCase("Stable") 
                    ? "Employment status indicates stability." 
                    : "Employment history shows insufficient stability.";
                    
            factors.add(createFactor(result, "Employment Stability", employmentImpact, employmentDescription));
            System.out.println("Added Employment Stability factor: " + employmentImpact + " - " + employmentDescription);
    
            // Payment History factor - handle different possible field names
            String paymentHistoryRating = "Fair";
            boolean isOnTime = false;
            
            if (creditEvaluationData.containsKey("paymentHistoryRating")) {
                paymentHistoryRating = (String) creditEvaluationData.get("paymentHistoryRating");
                isOnTime = "Excellent".equalsIgnoreCase(paymentHistoryRating);
            } else if (creditEvaluationData.containsKey("payment_history_rating")) {
                paymentHistoryRating = (String) creditEvaluationData.get("payment_history_rating");
                isOnTime = "Excellent".equalsIgnoreCase(paymentHistoryRating);
            } else if (creditEvaluationData.containsKey("payment_history")) {
                String paymentHistory = (String) creditEvaluationData.get("payment_history");
                // Check if it's strictly "On-time" or "On Time", anything else is considered late
                isOnTime = paymentHistory.equalsIgnoreCase("On-time") || 
                           paymentHistory.equalsIgnoreCase("On Time");
                
                // Convert payment history to rating
                if (isOnTime) {
                    paymentHistoryRating = "Excellent";
                } else {
                    // Any payment history that's not on-time is considered negative
                    paymentHistoryRating = "Poor";
                }
                
                System.out.println("Payment history value from data: '" + paymentHistory + 
                                 "', evaluated as: " + (isOnTime ? "On Time" : "Late") + 
                                 ", Rating: " + paymentHistoryRating);
            }
            
            String paymentHistoryImpact = isOnTime ? "Positive" : "Negative";
            String paymentHistoryDescription = isOnTime 
                    ? "Payment history shows consistent on-time payments." 
                    : "Payment history indicates late payments, which negatively impacts your credit assessment.";
                    
            factors.add(createFactor(result, "Payment History", paymentHistoryImpact, paymentHistoryDescription));
            System.out.println("Added Payment History factor: " + paymentHistoryImpact + " - " + paymentHistoryDescription);
    
            // Credit Score Accuracy (only if user provided a score)
            if (creditEvaluationData.containsKey("isScoreAccurate")) {
                boolean isScoreAccurate = (boolean) creditEvaluationData.get("isScoreAccurate");
                if (!isScoreAccurate) {
                    String message = (String) creditEvaluationData.get("creditScoreAccuracyMessage");
                    factors.add(createFactor(result, "Credit Score Discrepancy", "Warning", message));
                    System.out.println("Added Credit Score Discrepancy factor: Warning - " + message);
                }
            }
            
            // Credit Utilization factor (if available)
            if (creditEvaluationData.containsKey("creditUtilization") || 
                creditEvaluationData.containsKey("credit_utilization")) {
                
                double utilization = creditEvaluationData.containsKey("creditUtilization") ?
                    getDoubleValue(creditEvaluationData, "creditUtilization", 0.0) :
                    getDoubleValue(creditEvaluationData, "credit_utilization", 0.0);
                
                String utilizationImpact = utilization < 30 ? "Positive" : utilization < 70 ? "Neutral" : "Negative";
                String utilizationDescription = 
                    utilization < 30 ? 
                        "Credit utilization of " + String.format("%.1f%%", utilization) + " is low, which is positive." :
                    utilization < 70 ? 
                        "Credit utilization of " + String.format("%.1f%%", utilization) + " is moderate." :
                        "Credit utilization of " + String.format("%.1f%%", utilization) + " is high, which is negative.";
                
                factors.add(createFactor(result, "Credit Utilization", utilizationImpact, utilizationDescription));
                System.out.println("Added Credit Utilization factor: " + utilizationImpact + " - " + utilizationDescription);
            }
            
            // Save all factors
            decisionFactorRepository.saveAll(factors);
            System.out.println("Saved " + factors.size() + " decision factors");
        } catch (Exception e) {
            System.out.println("Error creating decision factors: " + e.getMessage());
            e.printStackTrace();
            
            // Add a generic factor so we have something
            factors.add(createFactor(result, "Application Review", "Neutral", 
                "Your application has been reviewed based on available information."));
            decisionFactorRepository.saveAll(factors);
        }
    }

    private DecisionFactor createFactor(LoanApplicationResult result, String factor, String impact, String description) {
        DecisionFactor decisionFactor = new DecisionFactor();
        decisionFactor.setResult(result);
        decisionFactor.setFactor(factor);
        decisionFactor.setImpact(impact);
        decisionFactor.setDescription(description);
        return decisionFactor;
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
                System.out.println("Failed to parse integer value for key '" + key + "': " + e.getMessage());
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
                System.out.println("Failed to parse double value for key '" + key + "': " + e.getMessage());
            }
        }
        return defaultValue;
    }

    /**
     * Get all decision factors for a loan application result
     */
    public List<DecisionFactor> getDecisionFactorsByResult(LoanApplicationResult result) {
        return decisionFactorRepository.findByResult(result);
    }

    /**
     * Get all decision factors for a loan application result by result ID
     */
    public List<DecisionFactor> getDecisionFactorsByLoanApplicationResultId(Long resultId) {
        return decisionFactorRepository.findByResultId(resultId);
    }
} 