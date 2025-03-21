package com.team1_5.credwise.config;

import com.team1_5.credwise.dto.CreditScoreDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration and Validation Component for Credit Score Assessment
 *
 * This class provides configuration and preliminary validation for credit scores.
 * It serves as a placeholder for more advanced credit scoring mechanisms.
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Component
public class CreditScoreValidator {
    private static final Logger logger = LoggerFactory.getLogger(CreditScoreValidator.class);

    // Global configuration for credit score thresholds
    public static class CreditScoreThresholds {
        // Minimum acceptable credit score
        public static final int MINIMUM_SCORE = 600;

        // Excellent credit score range
        public static final int EXCELLENT_SCORE_MIN = 750;

        // Good credit score range
        public static final int GOOD_SCORE_MIN = 700;
        public static final int GOOD_SCORE_MAX = 749;

        // Fair credit score range
        public static final int FAIR_SCORE_MIN = 650;
        public static final int FAIR_SCORE_MAX = 699;

        // Poor credit score range
        public static final int POOR_SCORE_MIN = 600;
        public static final int POOR_SCORE_MAX = 649;
    }

    /**
     * Preliminary credit score validation method
     *
     * @param creditScoreDetails Comprehensive credit score details
     * @return Boolean indicating preliminary credit worthiness
     */
    public boolean validateCreditScore(CreditScoreDetails creditScoreDetails) {
        // Validate input
        if (creditScoreDetails == null) {
            logger.warn("Null credit score details provided");
            return false;
        }

        try {
            // Log incoming credit score details for audit trail
            logCreditScoreDetails(creditScoreDetails);

            // Temporary implementation - always return true
            // Future: Implement comprehensive scoring logic
            boolean isValid = performPreliminaryValidation(creditScoreDetails);

            logger.info("Credit Score Validation Result: {}", isValid);
            return isValid;

        } catch (Exception e) {
            logger.error("Unexpected error during credit score validation", e);
            return false;
        }
    }

    /**
     * Perform preliminary validation of credit score details
     *
     * @param details Credit score details
     * @return Boolean indicating preliminary validation status
     */
    private boolean performPreliminaryValidation(CreditScoreDetails details) {
        // Placeholder validation logic
        // Future: Replace with comprehensive validation
        return true;
    }

    /**
     * Log credit score details for audit and monitoring
     *
     * @param details Credit score details to be logged
     */
    private void logCreditScoreDetails(CreditScoreDetails details) {
        logger.info("Credit Score Details Received:");
        logger.info("Credit Score: {}", details.getCreditScore());
        logger.info("Annual Income: {}", details.getAnnualIncome());
        logger.info("Credit Utilization: {}%", details.getCreditUtilization());
        logger.info("Number of Open Accounts: {}", details.getNumOpenAccounts());
    }

    /**
     * Categorize credit score
     *
     * @param score Numeric credit score
     * @return Credit score category
     */
    public String getCreditScoreCategory(int score) {
        if (score >= CreditScoreThresholds.EXCELLENT_SCORE_MIN) {
            return "EXCELLENT";
        } else if (score >= CreditScoreThresholds.GOOD_SCORE_MIN && score <= CreditScoreThresholds.GOOD_SCORE_MAX) {
            return "GOOD";
        } else if (score >= CreditScoreThresholds.FAIR_SCORE_MIN && score <= CreditScoreThresholds.FAIR_SCORE_MAX) {
            return "FAIR";
        } else if (score >= CreditScoreThresholds.POOR_SCORE_MIN && score <= CreditScoreThresholds.POOR_SCORE_MAX) {
            return "POOR";
        } else {
            return "INSUFFICIENT";
        }
    }

    /**
     * Calculate debt-to-income ratio
     *
     * @param monthlyIncome Monthly income
     * @param monthlyDebt Monthly debt payments
     * @return Debt-to-income ratio percentage
     */
    public BigDecimal calculateDebtToIncomeRatio(BigDecimal monthlyIncome, BigDecimal monthlyDebt) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid monthly income provided for debt-to-income calculation");
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal ratio = monthlyDebt.divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            logger.info("Debt-to-Income Ratio: {}%", ratio);
            return ratio;
        } catch (Exception e) {
            logger.error("Error calculating debt-to-income ratio", e);
            return BigDecimal.ZERO;
        }
    }
}