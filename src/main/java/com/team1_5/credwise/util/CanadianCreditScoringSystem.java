package com.team1_5.credwise.util;

import java.util.*;

public class CanadianCreditScoringSystem {

    // Credit score ranges
    public static final int MIN_CREDIT_SCORE = 300;
    public static final int MAX_CREDIT_SCORE = 900;

    // Main evaluation method
    public static String evaluateCreditProfile(Map<String, Object> profileData) {
        // Validation
        if (!validateInputData(profileData)) {
            return "ERROR: Invalid or missing input parameters";
        }

        // Extract parameters from the HashMap
        String loanType = (String) profileData.get("loanType");
        double income = getDoubleValue(profileData, "income");
        double expenses = getDoubleValue(profileData, "expenses");
        double debt = getDoubleValue(profileData, "debt");
        double loanRequest = getDoubleValue(profileData, "loanRequest");
        int tenure = getIntValue(profileData, "tenure");
        String paymentHistory = (String) profileData.get("paymentHistory");
        double usedCredit = getDoubleValue(profileData, "usedCredit");
        double creditLimit = getDoubleValue(profileData, "creditLimit");
        String employmentStatus = (String) profileData.get("employmentStatus");
        int monthsEmployed = getIntValue(profileData, "monthsEmployed");
        double assets = getDoubleValue(profileData, "assets");
        int bankAccounts = getIntValue(profileData, "bankAccounts");
        Set<String> debtTypes = (Set<String>) profileData.get("debtTypes");

        // Prepare data map for credit score calculation
        Map<String, Object> data = new HashMap<>();
        data.put("income", income);
        data.put("expenses", expenses);
        data.put("debt", debt);
        data.put("loan_request", loanRequest);
        data.put("payment_history", paymentHistory);
        data.put("used_credit", usedCredit);
        data.put("credit_limit", creditLimit);
        data.put("employment_status", employmentStatus);
        data.put("months_employed", monthsEmployed);
        data.put("total_assets", assets);
        data.put("debt_types", debtTypes);
        data.put("bank_accounts", bankAccounts);

        // Calculate key metrics
        int creditScore = calculateCreditScore(data);
        double dti = dtiScore(income, expenses, debt, loanRequest);
        double utilization = usedCredit / creditLimit;
        int eligibility = eligibilityScore(creditScore, dti, paymentHistory, monthsEmployed);

        // Determine loan decision
        boolean restricted = isLoanRestricted(loanType, employmentStatus, monthsEmployed);
        String decision = determineDecision(restricted, creditScore, dti, paymentHistory, utilization);

        // Calculate loan details
        double approvedAmount = decision.equals("Approved") ? loanRequest : 0.0;
        double baseRate = getBaseInterestRate(loanType);
        double interest = adjustInterestRate(baseRate, creditScore, tenure);
        double emi = (decision.equals("Approved") && approvedAmount >= 100) ?
                calculateEMI(approvedAmount, interest, tenure) : 0.0;

        // Generate formatted report
        StringBuilder report = new StringBuilder();
        report.append("\n=== Credit Evaluation Report ===\n");
        report.append(String.format("Credit Score: %d (%s)\n", 
            creditScore, creditScoreRating(creditScore)));
        report.append("Loan Type: ").append(loanType).append("\n");
        report.append("Loan Decision: ").append(decision).append("\n");
        report.append(String.format("Approved Amount: $%.2f\n", approvedAmount));
        report.append(String.format("Interest Rate: %.2f%%\n", interest * 100));
        report.append("EMI (" + tenure + " months): " + 
            (emi > 0 ? String.format("$%.2f", emi) : "N/A") + "\n");
        report.append("Eligibility Score: ").append(eligibility).append("/100\n");

        // Credit utilization details
        double utilizationPercent = (creditLimit > 0) ? (usedCredit / creditLimit) * 100 : 0;
        report.append(String.format("Credit Utilization: %.1f%% (%s)\n", 
            utilizationPercent, 
            utilizationPercent < 30 ? "Good" : utilizationPercent < 75 ? "Fair" : "High"));

        // Payment History Rating
        String paymentHistoryRating = determinePaymentHistoryRating(paymentHistory);
        report.append("Payment History Rating: ").append(paymentHistoryRating).append("\n");

        // Debt-to-Income Ratio
        report.append(String.format("Debt-to-Income Ratio: %.1f%% (%s)\n", 
            dti * 100, dti < 0.36 ? "Good" : dti < 0.43 ? "Fair" : "High"));

        // Employment Stability
        String employmentStability = determineEmploymentStability(employmentStatus, monthsEmployed);
        report.append("Employment Stability: ").append(employmentStability).append("\n");

        // Credit Age Estimate
        report.append("Credit Age Estimate: ").append(monthsEmployed).append(" months\n");

        // Asset to Debt Ratio
        double assetToDebtRatio = debt > 0 ? assets / debt : assets > 0 ? 999 : 0;
        report.append(String.format("Asset-to-Debt Ratio: %.1f (%s)\n", 
            assetToDebtRatio, 
            assetToDebtRatio > 2 ? "Excellent" : assetToDebtRatio > 1 ? "Good" : "Needs Improvement"));

        return report.toString();
    }

    // Helper method to determine payment history rating
    public static String determinePaymentHistoryRating(String paymentHistory) {
        if (paymentHistory == null) {
            return "Fair"; // Default rating if no data
        }
        
        String normalizedHistory = paymentHistory.trim().toLowerCase();
        
        // Handle various formats of "On-time"
        if (normalizedHistory.contains("on-time") || 
            normalizedHistory.contains("ontime") || 
            normalizedHistory.contains("on time") ||
            normalizedHistory.equals("excellent")) {
            return "Excellent";
        }
        
        // Handle various formats of "Late < 30"
        if (normalizedHistory.contains("< 30") || 
            normalizedHistory.contains("late < 30") || 
            normalizedHistory.contains("less than 30") ||
            normalizedHistory.contains("fair")) {
            return "Fair";
        }
        
        // Handle various formats of "Late 30-60"
        if (normalizedHistory.contains("30-60") || 
            normalizedHistory.contains("30 to 60") || 
            normalizedHistory.contains("between 30 and 60")) {
            return "Fair";
        }
        
        // Handle various formats of "Late > 60"
        if (normalizedHistory.contains("> 60") || 
            normalizedHistory.contains("more than 60") || 
            normalizedHistory.contains("greater than 60") ||
            normalizedHistory.contains("60+") ||
            normalizedHistory.contains("poor")) {
            return "Poor";
        }
        
        // Default to Fair for unknown formats
        return "Fair";
    }

    // Helper method to determine employment stability
    public static String determineEmploymentStability(String employmentStatus, int monthsEmployed) {
        return ((employmentStatus.equals("Full-time") && monthsEmployed >= 12) ? "Stable" : "Unstable or Student");
    }

    // Validation helper method
    private static boolean validateInputData(Map<String, Object> data) {
        String[] requiredKeys = {
            "loanType", "income", "expenses", "debt", "loanRequest", 
            "tenure", "paymentHistory", "usedCredit", "creditLimit", 
            "employmentStatus", "monthsEmployed", "assets", 
            "bankAccounts", "debtTypes"
        };

        for (String key : requiredKeys) {
            if (!data.containsKey(key) || data.get(key) == null) {
                System.out.println("Missing or null value for: " + key);
                return false;
            }
        }

        return true;
    }

    // Helper methods for safe type casting
    private static double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalArgumentException("Invalid double value for key: " + key);
    }

    private static int getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalArgumentException("Invalid integer value for key: " + key);
    }

    // Credit Scoring Methods
    public static double creditUtilizationScore(double used, double limit) {
        double ratio = (limit > 0) ? used / limit : 1.0;
        return Math.max(0, Math.min(1, 1 - ratio));
    }

    public static double dtiScore(double income, double expenses, double debt, double loanRequest) {
        return (income > 0) ? (expenses + debt + 0.03 * loanRequest) / (income * 12) : 1.0;
    }

    public static double paymentHistoryScore(String status) {
        switch (status) {
            case "On-time": return 1.0;
            case "Late < 60": return 0.5;
            default: return 0.0;
        }
    }

    public static double employmentScore(String status, int months) {
        return (status.equals("Full-time") && months >= 12) ? 1.0 : 0.5;
    }

    public static double assetsScore(double assets, double income) {
        return (income > 0) ? Math.min(1.0, assets / (income * 12)) : 0.0;
    }

    public static double creditMixScore(Set<String> debtTypes, int bankAccounts) {
        return Math.min(1.0, (debtTypes.size() + Math.min(bankAccounts, 3)) / 6.0);
    }

    public static int calculateCreditScore(Map<String, Object> data) {
        // Extract data for comprehensive scoring
        String paymentHistory = (String) data.get("payment_history");
        double usedCredit = (double) data.get("used_credit");
        double creditLimit = (double) data.get("credit_limit");
        
        // Convert debtTypes to accountTypes if available
        List<String> accountTypes = new ArrayList<>();
        if (data.containsKey("debt_types") && data.get("debt_types") instanceof Set) {
            Set<String> debtTypes = (Set<String>) data.get("debt_types");
            accountTypes.addAll(debtTypes);
        }
        
        // Credit inquiries - if available
        List<Date> inquiryDates = new ArrayList<>();
        if (data.containsKey("inquiry_dates") && data.get("inquiry_dates") instanceof List) {
            inquiryDates = (List<Date>) data.get("inquiry_dates");
        }
        
        // Historical scores - if available
        List<Integer> historicalScores = new ArrayList<>();
        if (data.containsKey("historical_scores") && data.get("historical_scores") instanceof List) {
            historicalScores = (List<Integer>) data.get("historical_scores");
        }
        
        // Credit age - if available, otherwise estimate from employment
        int creditAge = 0;
        if (data.containsKey("credit_age") && data.get("credit_age") instanceof Number) {
            creditAge = ((Number) data.get("credit_age")).intValue();
        } else if (data.containsKey("months_employed") && data.get("months_employed") instanceof Number) {
            // Estimate credit age based on employment duration if not available
            creditAge = ((Number) data.get("months_employed")).intValue();
        }
        
        // Use the comprehensive scoring method
        return calculateComprehensiveScore(
            paymentHistory,
            usedCredit,
            creditLimit,
            accountTypes,
            inquiryDates,
            historicalScores,
            creditAge
        );
    }

    public static String creditScoreRating(int score) {
        if (score < 560) return "Poor";
        if (score < 660) return "Fair";
        if (score < 725) return "Good";
        if (score < 800) return "Very Good";
        return "Excellent";
    }

    // Loan and Interest Rate Methods
    public static double getBaseInterestRate(String loanType) {
        switch (loanType) {
            case "Mortgage": return 0.065;
            case "Car Loan": return 0.08;
            case "Personal Loan": return 0.10;
            case "Student Loan": return 0.05;
            case "Credit Card": return 0.1999;
            default: return 0.10;
        }
    }

    public static double adjustInterestRate(double base, int score, int months) {
        if (score < 600) base += 0.02;
        else if (score < 660) base += 0.01;

        if (months > 60) base += 0.005;
        return Math.min(Math.max(base, 0.03), 0.25);
    }

    public static boolean isLoanRestricted(String loanType, String employmentStatus, int months) {
        if (employmentStatus.equals("Student") && months < 12) {
            return loanType.equals("Mortgage") || loanType.equals("Car Loan") || loanType.equals("Personal Loan");
        }
        return false;
    }

    public static double calculateEMI(double principal, double rate, int months) {
        double r = rate / 12;
        return (principal * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
    }

    public static int eligibilityScore(int score, double dti, String history, int months) {
        int base = (int) ((score - 300) / 6.0);  // Normalize to ~100
        if (dti < 0.3) base += 5;
        if (history.equals("On-time")) base += 5;
        if (months >= 12) base += 5;
        return Math.min(base, 100);
    }

    // Decision Making Methods
    private static String determineDecision(boolean restricted, int creditScore, double dti, 
                                            String history, double utilization) {
        if (restricted) return "Denied";
        if (creditScore >= 660 && dti <= 0.40 && history.equals("On-time")) return "Approved";
        if (creditScore < 500 || dti > 0.50 || utilization > 0.80) return "Denied";
        return "Review Manually";
    }

    // Conversion method for ArrayList
    public static Map<String, Object> convertListToMap(List<Object> profileDataList) {
        if (profileDataList.size() != 14) {
            throw new IllegalArgumentException("Invalid list size. Expected 14 elements.");
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("loanType", profileDataList.get(0));
        profileData.put("income", profileDataList.get(1));
        profileData.put("expenses", profileDataList.get(2));
        profileData.put("debt", profileDataList.get(3));
        profileData.put("loanRequest", profileDataList.get(4));
        profileData.put("tenure", profileDataList.get(5));
        profileData.put("paymentHistory", profileDataList.get(6));
        profileData.put("usedCredit", profileDataList.get(7));
        profileData.put("creditLimit", profileDataList.get(8));
        profileData.put("employmentStatus", profileDataList.get(9));
        profileData.put("monthsEmployed", profileDataList.get(10));
        profileData.put("assets", profileDataList.get(11));
        profileData.put("bankAccounts", profileDataList.get(12));
        profileData.put("debtTypes", profileDataList.get(13));

        return profileData;
    }

    /**
     * Applies account type weighting to a base credit score.
     * Different types of credit accounts have different impacts on overall credit worthiness.
     * 
     * @param baseScore The calculated base score
     * @param accountTypes List of account types (credit cards, loans, mortgages, etc.)
     * @return Weighted score after applying account type adjustments
     */
    public static int applyAccountTypeWeighting(int baseScore, List<String> accountTypes) {
        if (accountTypes == null || accountTypes.isEmpty()) {
            return baseScore;
        }
        
        int adjustment = 0;
        boolean hasMortgage = false;
        boolean hasInstallmentLoan = false;
        boolean hasCreditCard = false;
        boolean hasLineOfCredit = false;
        
        for (String accountType : accountTypes) {
            if (accountType == null) continue;
            
            String type = accountType.trim().toLowerCase();
            
            if (type.contains("mortgage")) {
                hasMortgage = true;
            }
            if (type.contains("loan") || type.contains("auto") || type.contains("car")) {
                hasInstallmentLoan = true;
            }
            if (type.contains("credit card")) {
                hasCreditCard = true;
            }
            if (type.contains("line of credit") || type.contains("loc")) {
                hasLineOfCredit = true;
            }
        }
        
        // Credit mix diversity bonus (up to 30 points)
        int creditMixCount = 0;
        if (hasMortgage) creditMixCount++;
        if (hasInstallmentLoan) creditMixCount++;
        if (hasCreditCard) creditMixCount++;
        if (hasLineOfCredit) creditMixCount++;
        
        // Award points based on diversity of credit mix (good for score)
        switch (creditMixCount) {
            case 1: adjustment += 5; break;
            case 2: adjustment += 15; break;
            case 3: adjustment += 25; break;
            case 4: adjustment += 30; break;
            default: adjustment += 0;
        }
        
        // Apply the adjustment ensuring we don't exceed max score
        int weightedScore = baseScore + adjustment;
        return Math.min(weightedScore, MAX_CREDIT_SCORE);
    }

    /**
     * Calculates credit utilization ratio with more precision.
     * Credit utilization has a significant impact on credit scores.
     * 
     * @param currentBalance Total current balance across all revolving credit accounts
     * @param creditLimit Total credit limit across all revolving credit accounts
     * @return A score component based on the calculated utilization ratio (0-100)
     */
    public static int calculateUtilizationScore(double currentBalance, double creditLimit) {
        if (creditLimit <= 0) {
            return 0; // Can't calculate utilization without a credit limit
        }
        
        // Calculate utilization as a percentage
        double utilizationRatio = (currentBalance / creditLimit) * 100;
        
        // Apply the Canadian scoring model for utilization
        // Lower utilization is better for credit score
        if (utilizationRatio < 10) {
            return 100; // Excellent: under 10%
        } else if (utilizationRatio < 30) {
            return 90; // Very good: 10-30%
        } else if (utilizationRatio < 50) {
            return 70; // Good: 30-50%
        } else if (utilizationRatio < 75) {
            return 40; // Fair: 50-75%
        } else if (utilizationRatio < 100) {
            return 20; // Poor: 75-100%
        } else {
            return 0; // Very poor: over 100% (maxed out)
        }
    }

    /**
     * Calculates a stabilized credit score by considering historical scores.
     * This helps smooth out temporary fluctuations for a more reliable score.
     * 
     * @param currentScore The newly calculated credit score
     * @param historicalScores List of previous credit scores (most recent first)
     * @return A stabilized credit score
     */
    public static int calculateStabilizedScore(int currentScore, List<Integer> historicalScores) {
        if (historicalScores == null || historicalScores.isEmpty()) {
            return currentScore; // No history to stabilize with
        }
        
        // Calculate weighted average with most recent scores having higher weight
        double totalWeight = 0;
        double weightedSum = 0;
        
        // Current score gets highest weight
        double currentWeight = 1.0;
        weightedSum += currentScore * currentWeight;
        totalWeight += currentWeight;
        
        // Apply diminishing weights to historical scores (most recent first)
        double weightFactor = 0.8; // Each previous score has 80% of the weight of the one after it
        double weight = currentWeight * weightFactor;
        
        // Consider up to 5 historical scores at most
        int scoresToConsider = Math.min(historicalScores.size(), 5);
        
        for (int i = 0; i < scoresToConsider; i++) {
            Integer score = historicalScores.get(i);
            if (score != null) {
                weightedSum += score * weight;
                totalWeight += weight;
                weight *= weightFactor; // Reduce weight for older scores
            }
        }
        
        // Calculate the weighted average
        int stabilizedScore = (int) Math.round(weightedSum / totalWeight);
        
        // Ensure the score stays within valid range
        return Math.max(MIN_CREDIT_SCORE, Math.min(stabilizedScore, MAX_CREDIT_SCORE));
    }

    /**
     * Calculates the impact of credit inquiries on the credit score.
     * Multiple recent inquiries can negatively impact credit score.
     * 
     * @param inquiries List of inquiry dates (most recent first)
     * @return A penalty amount to subtract from the credit score (0-50)
     */
    public static int calculateInquiryImpact(List<Date> inquiries) {
        if (inquiries == null || inquiries.isEmpty()) {
            return 0; // No inquiries, no impact
        }
        
        // Count inquiries in the last 12 months
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.YEAR, -1);
        Date oneYearAgo = cal.getTime();
        
        int recentInquiries = 0;
        
        for (Date inquiry : inquiries) {
            if (inquiry != null && inquiry.after(oneYearAgo)) {
                recentInquiries++;
            }
        }
        
        // Calculate impact based on number of recent inquiries
        if (recentInquiries <= 1) {
            return 0; // 0-1 inquiries: minimal impact
        } else if (recentInquiries <= 3) {
            return 10; // 2-3 inquiries: small impact
        } else if (recentInquiries <= 6) {
            return 25; // 4-6 inquiries: moderate impact
        } else {
            return 50; // 7+ inquiries: significant impact
        }
    }

    /**
     * Calculates a comprehensive credit score using all available metrics.
     * This provides a more accurate and detailed credit score assessment.
     * 
     * @param paymentHistory Payment history string
     * @param currentBalance Total current balance
     * @param creditLimit Total credit limit
     * @param accountTypes List of account types
     * @param inquiryDates List of credit inquiry dates
     * @param historicalScores List of previous credit scores (optional)
     * @param creditAge Age of oldest credit account in months
     * @return A comprehensive credit score between MIN_CREDIT_SCORE and MAX_CREDIT_SCORE
     */
    public static int calculateComprehensiveScore(
            String paymentHistory,
            double currentBalance,
            double creditLimit,
            List<String> accountTypes,
            List<Date> inquiryDates,
            List<Integer> historicalScores,
            int creditAge) {
        
        // Base score components
        int paymentHistoryScore = 0;
        int utilizationScore = 0;
        int creditAgeScore = 0;
        int baseScore = 0;
        
        // Payment history (35% of score)
        String paymentRating = determinePaymentHistoryRating(paymentHistory);
        switch (paymentRating) {
            case "Excellent": paymentHistoryScore = 300; break;
            case "Good": paymentHistoryScore = 250; break;
            case "Fair": paymentHistoryScore = 175; break;
            case "Poor": paymentHistoryScore = 100; break;
            default: paymentHistoryScore = 150; // Default to middle score
        }
        
        // Credit utilization (30% of score)
        utilizationScore = calculateUtilizationScore(currentBalance, creditLimit);
        
        // Credit age (15% of score)
        if (creditAge < 6) {
            creditAgeScore = 30; // Less than 6 months
        } else if (creditAge < 24) {
            creditAgeScore = 60; // 6-24 months
        } else if (creditAge < 60) {
            creditAgeScore = 100; // 2-5 years
        } else if (creditAge < 120) {
            creditAgeScore = 125; // 5-10 years
        } else {
            creditAgeScore = 150; // 10+ years
        }
        
        // Calculate base score from main components
        baseScore = paymentHistoryScore + (utilizationScore * 3) + creditAgeScore;
        
        // Apply account type diversity adjustment
        int weightedScore = applyAccountTypeWeighting(baseScore, accountTypes);
        
        // Subtract penalty for recent inquiries
        int inquiryPenalty = calculateInquiryImpact(inquiryDates);
        int adjustedScore = weightedScore - inquiryPenalty;
        
        // Ensure score is within valid range
        adjustedScore = Math.max(MIN_CREDIT_SCORE, Math.min(adjustedScore, MAX_CREDIT_SCORE));
        
        // Apply historical stabilization if available
        return calculateStabilizedScore(adjustedScore, historicalScores);
    }

    /**
     * Predicts future credit score improvements based on hypothetical changes.
     * Helps users understand what actions would most improve their score.
     * 
     * @param currentScore The current credit score
     * @param currentUtilization Current credit utilization percentage
     * @param paymentHistory Current payment history
     * @param accountTypes Current account types
     * @param creditAge Current credit age in months
     * @return Map of improvement scenarios with predicted score changes
     */
    public static Map<String, Integer> predictScoreImprovements(
            int currentScore,
            double currentUtilization,
            String paymentHistory,
            List<String> accountTypes,
            int creditAge) {
        
        Map<String, Integer> predictions = new HashMap<>();
        
        // Predict score if utilization is reduced to 10%
        if (currentUtilization > 10) {
            int utilizationImprovement = 0;
            
            if (currentUtilization > 75) {
                utilizationImprovement = 80; // Major improvement
            } else if (currentUtilization > 50) {
                utilizationImprovement = 60; // Significant improvement
            } else if (currentUtilization > 30) {
                utilizationImprovement = 40; // Moderate improvement
            } else if (currentUtilization > 10) {
                utilizationImprovement = 20; // Small improvement
            }
            
            predictions.put("Reduce credit utilization to 10%", 
                    Math.min(currentScore + utilizationImprovement, MAX_CREDIT_SCORE));
        }
        
        // Predict score if payment history improves over time
        String paymentRating = determinePaymentHistoryRating(paymentHistory);
        if (!paymentRating.equals("Excellent")) {
            int historyImprovement = 0;
            
            if (paymentRating.equals("Poor")) {
                historyImprovement = 100; // Major improvement over 1-2 years
            } else if (paymentRating.equals("Fair")) {
                historyImprovement = 50; // Moderate improvement over 1 year
            }
            
            predictions.put("Maintain perfect payment history for 1+ year", 
                    Math.min(currentScore + historyImprovement, MAX_CREDIT_SCORE));
        }
        
        // Predict score if adding a new account type they don't have
        boolean hasMortgage = false;
        boolean hasLoan = false;
        boolean hasCreditCard = false;
        boolean hasLineOfCredit = false;
        
        for (String type : accountTypes) {
            if (type == null) continue;
            String normalizedType = type.toLowerCase();
            if (normalizedType.contains("mortgage")) hasMortgage = true;
            if (normalizedType.contains("loan")) hasLoan = true;
            if (normalizedType.contains("credit card")) hasCreditCard = true;
            if (normalizedType.contains("line of credit")) hasLineOfCredit = true;
        }
        
        // Suggest account types they don't have
        int accountDiversityImprovement = 15;
        if (!hasMortgage && !hasLoan) {
            predictions.put("Add an installment loan", 
                    Math.min(currentScore + accountDiversityImprovement, MAX_CREDIT_SCORE));
        }
        
        if (!hasCreditCard) {
            predictions.put("Open a credit card account", 
                    Math.min(currentScore + accountDiversityImprovement, MAX_CREDIT_SCORE));
        }
        
        // Predict score improvement as credit history ages
        if (creditAge < 24) {
            predictions.put("Wait for credit history to age 2+ years", 
                    Math.min(currentScore + 30, MAX_CREDIT_SCORE));
        } else if (creditAge < 60) {
            predictions.put("Wait for credit history to age 5+ years", 
                    Math.min(currentScore + 20, MAX_CREDIT_SCORE));
        }
        
        return predictions;
    }
} 