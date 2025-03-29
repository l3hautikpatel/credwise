package com.team1_5.credwise.util;

import java.util.*;

public class CanadianCreditScoringSystem {

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
        report.append("Debt Ratio: ").append(dti < 0.4 ? "Positive" : "Negative").append("\n");
        
        // Payment History Rating
        String paymentHistoryRating = determinePaymentHistoryRating(paymentHistory);
        report.append("Payment History Rating: ").append(paymentHistoryRating).append("\n");
        
        // Employment Stability
        String employmentStability = determineEmploymentStability(employmentStatus, monthsEmployed);
        report.append("Employment Stability: ").append(employmentStability).append("\n");

        return report.toString();
    }

    // Helper method to determine payment history rating
    public static String determinePaymentHistoryRating(String paymentHistory) {
        if (paymentHistory.equals("On-time")) {
            return "Excellent";
        } else if (paymentHistory.equals("Late < 60")) {
            return "Fair";
        } else {
            return "Poor";
        }
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
        // Canadian style: out of 900
        double score = 0.35 * paymentHistoryScore((String) data.get("payment_history")) +
                       0.30 * creditUtilizationScore((double) data.get("used_credit"), (double) data.get("credit_limit")) +
                       0.15 * (1 - dtiScore((double) data.get("income"), (double) data.get("expenses"), (double) data.get("debt"), (double) data.get("loan_request"))) +
                       0.10 * employmentScore((String) data.get("employment_status"), (int) data.get("months_employed")) +
                       0.05 * assetsScore((double) data.get("total_assets"), (double) data.get("income")) +
                       0.05 * creditMixScore((Set<String>) data.get("debt_types"), (int) data.get("bank_accounts"));

        return (int) Math.round(300 + 600 * score);
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
} 