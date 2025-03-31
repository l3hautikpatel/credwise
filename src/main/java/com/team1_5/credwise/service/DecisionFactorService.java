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
            String rawPaymentHistory = null;
            
            System.out.println("\n🔍 PAYMENT HISTORY CHECK: Starting payment history factor creation");
            System.out.println("Credit evaluation data keys: " + String.join(", ", creditEvaluationData.keySet()));
            
            // First check: Direct check for raw payment history
            if (creditEvaluationData.containsKey("payment_history")) {
                rawPaymentHistory = (String) creditEvaluationData.get("payment_history");
                System.out.println("🔍 FOUND payment_history key with value: '" + rawPaymentHistory + "'");
                
                // Explicit check for "Late" keyword - this should catch all possible formats
                if (rawPaymentHistory != null && 
                    (rawPaymentHistory.contains("Late") || rawPaymentHistory.contains("late"))) {
                    System.out.println("✅ DIRECT MATCH: Found 'Late' in payment history, setting NEGATIVE impact");
                    isOnTime = false;
                    paymentHistoryRating = "Poor";
                } else if (rawPaymentHistory != null && 
                          (rawPaymentHistory.equalsIgnoreCase("On-time") || 
                           rawPaymentHistory.equalsIgnoreCase("On Time") ||
                           rawPaymentHistory.equals("On time"))) {
                    System.out.println("✅ DIRECT MATCH: Found on-time payment history, setting POSITIVE impact");
                    isOnTime = true;
                    paymentHistoryRating = "Excellent";
                } else if (rawPaymentHistory != null) {
                    // Default to negative for any unknown payment history
                    System.out.println("⚠️ UNKNOWN: Payment history is not explicitly on-time, defaulting to NEGATIVE: '" + 
                                     rawPaymentHistory + "'");
                    isOnTime = false;
                    paymentHistoryRating = "Poor";
                }
            } 
            // Second check: Look for payment history rating
            else if (creditEvaluationData.containsKey("payment_history_rating")) {
                paymentHistoryRating = (String) creditEvaluationData.get("payment_history_rating");
                System.out.println("🔍 FOUND payment_history_rating: '" + paymentHistoryRating + "'");
                isOnTime = "Excellent".equalsIgnoreCase(paymentHistoryRating);
            } 
            // Third check: Look for original raw payment history key
            else if (creditEvaluationData.containsKey("paymentHistory")) {
                rawPaymentHistory = (String) creditEvaluationData.get("paymentHistory");
                System.out.println("🔍 FOUND paymentHistory key with value: '" + rawPaymentHistory + "'");
                
                // Same logic as above - check for "Late" explicitly
                if (rawPaymentHistory != null && 
                    (rawPaymentHistory.contains("Late") || rawPaymentHistory.contains("late"))) {
                    System.out.println("✅ DIRECT MATCH: Found 'Late' in paymentHistory, setting NEGATIVE impact");
                    isOnTime = false;
                    paymentHistoryRating = "Poor";
                } else if (rawPaymentHistory != null && 
                          (rawPaymentHistory.equalsIgnoreCase("On-time") || 
                           rawPaymentHistory.equalsIgnoreCase("On Time") ||
                           rawPaymentHistory.equals("On time"))) {
                    System.out.println("✅ DIRECT MATCH: Found on-time paymentHistory, setting POSITIVE impact");
                    isOnTime = true;
                    paymentHistoryRating = "Excellent";
                } else if (rawPaymentHistory != null) {
                    System.out.println("⚠️ UNKNOWN: paymentHistory is not explicitly on-time or late, defaulting to NEGATIVE: '" + 
                                     rawPaymentHistory + "'");
                    isOnTime = false;
                    paymentHistoryRating = "Poor";
                }
            } 
            // Last check: Check every key for anything containing "payment" and "history"
            else {
                System.out.println("⚠️ No direct payment history key found, checking all keys for relevant data");
                for (String key : creditEvaluationData.keySet()) {
                    if (key.toLowerCase().contains("payment") && 
                        (key.toLowerCase().contains("history") || key.toLowerCase().contains("status"))) {
                        Object value = creditEvaluationData.get(key);
                        System.out.println("🔍 Found potential payment history in key: '" + key + "' with value: '" + value + "'");
                        
                        if (value instanceof String) {
                            rawPaymentHistory = (String) value;
                            if (rawPaymentHistory.contains("Late") || rawPaymentHistory.contains("late")) {
                                System.out.println("✅ DIRECT MATCH: Found 'Late' in key '" + key + "', setting NEGATIVE impact");
                                isOnTime = false;
                                paymentHistoryRating = "Poor";
                                break;
                            } else if (rawPaymentHistory.equalsIgnoreCase("On-time") || 
                                      rawPaymentHistory.equalsIgnoreCase("On Time")) {
                                System.out.println("✅ DIRECT MATCH: Found on-time payment in key '" + key + "', setting POSITIVE impact");
                                isOnTime = true;
                                paymentHistoryRating = "Excellent";
                                break;
                            }
                        }
                    } else if (key.equalsIgnoreCase("paymentHistoryRating")) {
                        // Special case for paymentHistoryRating field
                        String ratingValue = (String) creditEvaluationData.get(key);
                        System.out.println("🔍 Found payment history rating in key: '" + key + "' with value: '" + ratingValue + "'");
                        
                        if (ratingValue != null && ratingValue.equalsIgnoreCase("Excellent")) {
                            System.out.println("✅ RATING MATCH: Found Excellent payment history rating, setting POSITIVE impact");
                            isOnTime = true;
                            paymentHistoryRating = "Excellent";
                            break;
                        } else if (ratingValue != null && 
                                 (ratingValue.equalsIgnoreCase("Poor") || ratingValue.equalsIgnoreCase("Bad"))) {
                            System.out.println("✅ RATING MATCH: Found Poor/Bad payment history rating, setting NEGATIVE impact");
                            isOnTime = false;
                            paymentHistoryRating = "Poor";
                            break;
                        }
                    }
                }
                
                // If nothing found, default to negative
                if (rawPaymentHistory == null && paymentHistoryRating.equals("Fair")) {
                    System.out.println("⚠️ No payment history found in any key, defaulting to NEGATIVE impact");
                    isOnTime = false;
                    paymentHistoryRating = "Poor";
                }
            }
            
            // Final safety check - ONLY force negative impact if specifically contains "Late"
            if (rawPaymentHistory != null && 
                (rawPaymentHistory.contains("Late") || 
                 rawPaymentHistory.contains("late") || 
                 rawPaymentHistory.toLowerCase().contains("default") ||
                 rawPaymentHistory.toLowerCase().contains("missed") ||
                 rawPaymentHistory.toLowerCase().contains("delinquent"))) {
                System.out.println("🛑 FINAL SAFETY CHECK: Forcing NEGATIVE impact for late-indicating payment history: '" + 
                                 rawPaymentHistory + "'");
                isOnTime = false;
                paymentHistoryRating = "Poor";
            } else if (rawPaymentHistory != null &&
                      (rawPaymentHistory.equalsIgnoreCase("On-time") || 
                       rawPaymentHistory.equalsIgnoreCase("On Time") ||
                       rawPaymentHistory.equals("On time"))) {
                // SAFETY CHECK: Ensure "On Time" is ALWAYS treated as positive
                System.out.println("✅ FINAL SAFETY CHECK: Ensuring POSITIVE impact for on-time payment history: '" + 
                                 rawPaymentHistory + "'");
                isOnTime = true;
                paymentHistoryRating = "Excellent";
            } else if (rawPaymentHistory != null && 
                      !rawPaymentHistory.toLowerCase().contains("late")) {
                // If it doesn't explicitly mention "late", treat as positive
                System.out.println("✅ LENIENT CHECK: Payment history doesn't contain 'late', assuming positive: '" + 
                                 rawPaymentHistory + "'");
                isOnTime = true;
                paymentHistoryRating = "Excellent";
            }
            
            // Generate description and create factor
            String paymentHistoryImpact = "Neutral";
            String paymentHistoryDescription = "";
            
            // Extra logging to debug the decision
            System.out.println("🧪 DECISION DEBUG - Final values: isOnTime=" + isOnTime + 
                             ", paymentHistoryRating=" + paymentHistoryRating + 
                             ", rawPaymentHistory=" + rawPaymentHistory);
            
            // GUARANTEED LATE PAYMENT CHECK - if any form of "Late" is present, always force negative
            if (rawPaymentHistory != null && 
                (rawPaymentHistory.toLowerCase().contains("late") || 
                 rawPaymentHistory.toLowerCase().contains("default") ||
                 rawPaymentHistory.toLowerCase().contains("missed") ||
                 rawPaymentHistory.toLowerCase().contains("delinquent"))) {
                
                System.out.println("‼️ GUARANTEED NEGATIVE CHECK: Found late payment indicator, FORCING negative impact");
                isOnTime = false;
                paymentHistoryRating = "Poor";
                paymentHistoryImpact = "Negative";
                paymentHistoryDescription = "Payment history indicates late payments, which negatively impacts your credit assessment.";
            }
            // Determine impact based on rating and raw history if not already set
            else if (isOnTime || "Excellent".equalsIgnoreCase(paymentHistoryRating)) {
                paymentHistoryImpact = "Positive";
                paymentHistoryDescription = "Payment history shows consistent on-time payments.";
                System.out.println("✅ FINAL DECISION: Setting POSITIVE impact based on payment history");
            } else if (!isOnTime || "Poor".equalsIgnoreCase(paymentHistoryRating)) {
                paymentHistoryImpact = "Negative";
                paymentHistoryDescription = "Payment history indicates late payments, which negatively impacts your credit assessment.";
                System.out.println("⚠️ FINAL DECISION: Setting NEGATIVE impact based on payment history");
            } else {
                // Default neutral case
                paymentHistoryImpact = "Neutral";
                paymentHistoryDescription = "Payment history has been considered in your application assessment.";
                System.out.println("ℹ️ FINAL DECISION: Setting NEUTRAL impact due to ambiguous payment history");
            }
                    
            // Create the decision factor
            DecisionFactor paymentHistoryFactor = createFactor(result, "Payment History", paymentHistoryImpact, paymentHistoryDescription);
            
            // FINAL VALIDATION - Make absolutely sure late payments are negative
            if (rawPaymentHistory != null && rawPaymentHistory.toLowerCase().contains("late") && 
                !"Negative".equals(paymentHistoryFactor.getImpact())) {
                
                System.out.println("🚨 CRITICAL ERROR: Late payment wasn't marked as negative! Fixing before save.");
                paymentHistoryFactor.setImpact("Negative");
                paymentHistoryFactor.setDescription("Payment history indicates late payments, which negatively impacts your credit assessment.");
            }
            
            factors.add(paymentHistoryFactor);
            System.out.println("📊 PAYMENT HISTORY FACTOR CREATED: " + paymentHistoryImpact + 
                           " - " + paymentHistoryDescription + 
                           " (Based on: " + (rawPaymentHistory != null ? "'" + rawPaymentHistory + "'" : "'" + paymentHistoryRating + "'") + ")");
            System.out.println("💾 DATABASE VALUE: factor='Payment History', impact='" + paymentHistoryFactor.getImpact() + "'");
            
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