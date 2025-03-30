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
        return ((employmentStatus.equals("Full-time") && monthsEmployed >= 12) || monthsEmployed >= 24) ? "Stable" : "Unstable or Student";
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

    /**
     * Calculate Debt-to-Income ratio to assess borrower's ability to manage monthly payments
     * @param income Monthly income
     * @param expenses Monthly expenses
     * @param existingDebt Current total debt
     * @param loanAmount Amount being requested for loan
     * @return DTI ratio as a decimal (0.0-1.0)
     */
    public static double dtiScore(double income, double expenses, double existingDebt, double loanAmount) {
        try {
            // Calculate monthly debt payment (approximate using 5% of total debt as monthly payment)
            // This is a simplified estimation - in reality would depend on terms of each debt
            double existingMonthlyDebtPayment = existingDebt * 0.05;
            
            // Calculate estimated monthly payment for new loan (simplified using 2% of loan amount)
            double estimatedLoanMonthlyPayment = loanAmount * 0.02;
            
            // Total monthly obligations: expenses + existing debt payments + new loan payment
            double totalMonthlyObligations = expenses + existingMonthlyDebtPayment + estimatedLoanMonthlyPayment;
            
            // Calculate Debt-to-Income ratio
            double dti = (income > 0) ? (totalMonthlyObligations / income) : 1.0;
            
            // Ensure DTI is between 0 and 1 for calculation purposes
            return Math.max(0.0, Math.min(1.0, dti));
        } catch (Exception e) {
            System.out.println("Error calculating DTI score: " + e.getMessage());
            return 0.5; // Return moderate DTI on error
        }
    }

    public static double paymentHistoryScore(String status) {
        switch (status) {
            case "On-time": return 1.0;
            case "Late < 60": return 0.5;
            default: return 0.0;
        }
    }

    public static double employmentScore(String status, int months) {
        return ((status.equals("Full-time") && months >= 12) || months >= 24) ? 1.0 : 0.5;
    }

    public static double assetsScore(double assets, double income) {
        return (income > 0) ? Math.min(1.0, assets / (income * 12)) : 0.0;
    }

    public static double creditMixScore(Set<String> debtTypes, int bankAccounts) {
        return Math.min(1.0, (debtTypes.size() + Math.min(bankAccounts, 3)) / 6.0);
    }

    public static int calculateCreditScore(Map<String, Object> data) {
        // Add comprehensive debug to help troubleshoot the exact data we receive
        System.out.println("Calculating credit score with data: " + data);
        
        try {
            // Extract base metrics with proper defaults
            String paymentHistory = getStringValueSafely(data, "paymentHistory", "On-time");
            double usedCredit = getDoubleValueSafely(data, "usedCredit", 0.0);
            double creditLimit = getDoubleValueSafely(data, "creditLimit", 1000.0);
            String employmentStatus = getStringValueSafely(data, "employmentStatus", "Unemployed");
            int monthsEmployed = getIntValueSafely(data, "monthsEmployed", 0);
            double assets = getDoubleValueSafely(data, "assets", 0.0);
            double debt = getDoubleValueSafely(data, "debt", 0.0);
            double income = getDoubleValueSafely(data, "income", 3000.0); // Default reasonable income
            double expenses = getDoubleValueSafely(data, "expenses", 1500.0); // Default reasonable expenses
            
            // Gather debt types
            Set<String> debtTypes = new HashSet<>();
            if (data.containsKey("debtTypes") && data.get("debtTypes") instanceof Set) {
                debtTypes = (Set<String>) data.get("debtTypes");
            }
            
            // Convert to account types for scoring
            List<String> accountTypes = new ArrayList<>(debtTypes);
            
            // Initialize credit age based on employment if not directly provided
            int creditAge = getIntValueSafely(data, "creditAge", Math.max(6, monthsEmployed));
            
            // Initialize lists for other factors
            List<Date> inquiryDates = new ArrayList<>(); // Empty list as default
            List<Integer> historicalScores = new ArrayList<>(); // Empty list as default
            
            // Core score calculation components
            int paymentHistoryComponent = 0;     // 35% of score (0-300)
            int creditUtilizationComponent = 0;  // 30% of score (0-300)
            int creditAgeComponent = 0;          // 15% of score (0-150)
            int creditMixComponent = 0;          // 10% of score (0-100)
            int inquiriesComponent = 0;          // 10% of score (0-100)
            int baseScore = 0;
            
            // 1. Payment History (35%)
            String paymentRating = determinePaymentHistoryRating(paymentHistory);
            switch (paymentRating) {
                case "Excellent": paymentHistoryComponent = 300; break;
                case "Good": paymentHistoryComponent = 260; break;
                case "Fair": paymentHistoryComponent = 200; break;
                case "Poor": paymentHistoryComponent = 120; break;
                default: paymentHistoryComponent = 180; // Default to middle score
            }
            
            // 2. Credit Utilization (30%) - Lower is better
            double utilizationRatio = 0.0;
            if (creditLimit > 0) {
                utilizationRatio = (usedCredit / creditLimit) * 100; // Convert to percentage
                
                if (utilizationRatio <= 10) {
                    creditUtilizationComponent = 300; // Excellent: 0-10%
                } else if (utilizationRatio <= 30) {
                    creditUtilizationComponent = 270; // Very good: 10-30%
                } else if (utilizationRatio <= 50) {
                    creditUtilizationComponent = 210; // Good: 30-50%
                } else if (utilizationRatio <= 75) {
                    creditUtilizationComponent = 150; // Fair: 50-75%
                } else if (utilizationRatio < 100) {
                    creditUtilizationComponent = 90;  // Poor: 75-100%
                } else {
                    creditUtilizationComponent = 30;  // Very poor: over 100%
                }
            } else {
                // If no credit limit is available, assume moderate utilization
                creditUtilizationComponent = 150;
            }
            
            // 3. Credit Age (15%) - Longer is better
            if (creditAge < 6) {
                creditAgeComponent = 30;      // Very short history: <6 months
            } else if (creditAge < 24) {
                creditAgeComponent = 60;      // Short history: 6-24 months
            } else if (creditAge < 60) {
                creditAgeComponent = 100;     // Moderate history: 2-5 years
            } else if (creditAge < 120) {
                creditAgeComponent = 125;     // Good history: 5-10 years
            } else {
                creditAgeComponent = 150;     // Excellent history: 10+ years
            }
            
            // 4. Credit Mix (10%) - More diverse is better
            int differentAccountTypes = accountTypes.size();
            if (differentAccountTypes >= 4) {
                creditMixComponent = 100;     // Excellent mix: 4+ types
            } else if (differentAccountTypes == 3) {
                creditMixComponent = 85;      // Very good mix: 3 types
            } else if (differentAccountTypes == 2) {
                creditMixComponent = 70;      // Good mix: 2 types
            } else if (differentAccountTypes == 1) {
                creditMixComponent = 50;      // Fair mix: 1 type
            } else {
                creditMixComponent = 30;      // Poor mix: 0 types
            }
            
            // 5. New Credit Inquiries (10%) - Fewer is better
            // Since we don't have actual inquiry data, we'll use a default good score here
            inquiriesComponent = 90;          // Assume good (few inquiries)
            
            // 6. Employment Status and Income Stability Bonus
            int employmentBonus = 0;
            if (employmentStatus.equalsIgnoreCase("Full-time") && monthsEmployed >= 24) {
                employmentBonus = 40;         // Long-term full-time employment bonus
            } else if (employmentStatus.equalsIgnoreCase("Full-time") && monthsEmployed >= 12) {
                employmentBonus = 25;         // Full-time employment bonus
            } else if (employmentStatus.equalsIgnoreCase("Part-time") && monthsEmployed >= 12) {
                employmentBonus = 15;         // Part-time employment bonus
            } else if (monthsEmployed >= 36) {
                employmentBonus = 30;         // Long-term employment bonus (any type)
            } else if (monthsEmployed >= 24) {
                employmentBonus = 20;         // Medium-term employment bonus (any type)
            }
            
            // 7. Assets to Debt Ratio Bonus
            int assetBonus = 0;
            if (debt > 0 && assets > 0) {
                double assetToDebtRatio = assets / debt;
                if (assetToDebtRatio > 5) {
                    assetBonus = 30;          // Excellent asset coverage
                } else if (assetToDebtRatio > 2) {
                    assetBonus = 20;          // Good asset coverage
                } else if (assetToDebtRatio > 1) {
                    assetBonus = 10;          // Fair asset coverage
                }
            } else if (assets > 20000) {      // Good assets, no debt
                assetBonus = 30;
            }
            
            // 8. Calculate DTI (Debt-to-Income) Ratio Component
            int dtiComponent = 0;
            double loanRequest = getDoubleValueSafely(data, "loanRequest", 10000.0);
            double dti = dtiScore(income, expenses, debt, loanRequest);
            
            if (dti < 0.25) {
                dtiComponent = 40;            // Excellent DTI: below 25%
            } else if (dti < 0.36) {
                dtiComponent = 30;            // Very good DTI: 25-36%
            } else if (dti < 0.43) {
                dtiComponent = 20;            // Good DTI: 36-43%
            } else if (dti < 0.5) {
                dtiComponent = 10;            // Fair DTI: 43-50%
            } else {
                dtiComponent = 0;             // Poor DTI: above 50%
            }
            
            // Calculate base score from all components
            baseScore = paymentHistoryComponent + 
                        creditUtilizationComponent + 
                        creditAgeComponent + 
                        creditMixComponent + 
                        inquiriesComponent + 
                        employmentBonus + 
                        assetBonus + 
                        dtiComponent;
            
            // Normalize to 300-900 range (common credit score range)
            int minPossibleTotal = 120 + 30 + 30 + 30 + 0 + 0 + 0 + 0;     // 210 (worst case)
            int maxPossibleTotal = 300 + 300 + 150 + 100 + 100 + 40 + 30 + 40; // 1060 (best case)
            int range = maxPossibleTotal - minPossibleTotal;
            
            int normalizedScore = MIN_CREDIT_SCORE + 
                    (int)((baseScore - minPossibleTotal) * (MAX_CREDIT_SCORE - MIN_CREDIT_SCORE) / (double)range);
            
            // Ensure score is within valid range
            int finalScore = Math.max(MIN_CREDIT_SCORE, Math.min(normalizedScore, MAX_CREDIT_SCORE));
            
            // Log calculation details
            System.out.println("Credit Score Calculation Details:");
            System.out.println("Payment History: " + paymentHistory + " (Rating: " + paymentRating + ", Score: " + paymentHistoryComponent + ")");
            System.out.println("Credit Utilization: " + utilizationRatio + "% (Score: " + creditUtilizationComponent + ")");
            System.out.println("Credit Age: " + creditAge + " months (Score: " + creditAgeComponent + ")");
            System.out.println("Credit Mix: " + differentAccountTypes + " types (Score: " + creditMixComponent + ")");
            System.out.println("Employment: " + employmentStatus + ", " + monthsEmployed + " months (Bonus: " + employmentBonus + ")");
            System.out.println("Asset/Debt: Assets=" + assets + ", Debt=" + debt + " (Bonus: " + assetBonus + ")");
            System.out.println("DTI Ratio: " + (dti * 100) + "% (Score: " + dtiComponent + ")");
            System.out.println("Base Score: " + baseScore);
            System.out.println("Final Score: " + finalScore);
            
            return finalScore;
            
        } catch (Exception e) {
            System.out.println("Error calculating credit score: " + e.getMessage());
            e.printStackTrace();
            return 650; // Return a moderate default score on error
        }
    }
    
    /**
     * Helper method to safely extract string values from a map
     */
    private static String getStringValueSafely(Map<String, Object> data, String key, String defaultValue) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) {
            System.out.println("Warning: Missing or null value for key: " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
        
        Object value = data.get(key);
        if (value instanceof String) {
            return (String) value;
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Helper method to safely extract integer values from a map
     */
    private static int getIntValueSafely(Map<String, Object> data, String key, int defaultValue) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) {
            System.out.println("Warning: Missing or null value for key: " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
        
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                System.out.println("Warning: Could not parse string value for key: " + key + ", using default: " + defaultValue);
                return defaultValue;
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        
        System.out.println("Warning: Unexpected type for key: " + key + " (" + value.getClass().getName() + "), using default: " + defaultValue);
        return defaultValue;
    }
    
    /**
     * Helper method to safely extract double values from a map
     */
    private static double getDoubleValueSafely(Map<String, Object> data, String key, double defaultValue) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) {
            System.out.println("Warning: Missing or null value for key: " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
        
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                System.out.println("Warning: Could not parse string value for key: " + key + ", using default: " + defaultValue);
                return defaultValue;
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 1.0 : 0.0;
        }
        
        System.out.println("Warning: Unexpected type for key: " + key + " (" + value.getClass().getName() + "), using default: " + defaultValue);
        return defaultValue;
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
        // If employment is 24+ months, no restrictions regardless of status
        if (months >= 24) {
            return false;
        }
        
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