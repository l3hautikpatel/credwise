package com.team1_5.credwise.util;

import com.team1_5.credwise.model.LoanApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for integrating with Machine Learning model for loan eligibility prediction
 *
 * Handles communication with external ML model and processes loan application data
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Component
public class MLModelIntegration {
    private static final Logger logger = LoggerFactory.getLogger(MLModelIntegration.class);

    // Configuration for ML model endpoint
    @Value("${ml.model.endpoint:http://localhost:5000/predict}")
    private String mlModelEndpoint;

    private final RestTemplate restTemplate;

    public MLModelIntegration(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Represents the decision from the ML model
     */
    public static class LoanDecision {
        private boolean approved;
        private double approvalProbability;

        public LoanDecision(boolean approved, double approvalProbability) {
            this.approved = approved;
            this.approvalProbability = approvalProbability;
        }

        public boolean isApproved() {
            return approved;
        }

        public double getApprovalProbability() {
            return approvalProbability;
        }
    }

    /**
     * Predict loan eligibility using ML model
     *
     * @param loanApplication Loan application to evaluate
     * @return Loan decision with approval status and probability
     */
    public LoanDecision predictLoanEligibility(LoanApplication loanApplication) {
        try {
            // Prepare input for ML model
            Map<String, Object> mlInput = prepareMachineLearningInput(loanApplication);

            // Log input for debugging
            logModelInput(mlInput);

            // Call ML model endpoint
            LoanDecision decision = callMLModelEndpoint(mlInput);

            // Log decision for audit
            logModelDecision(decision);

            return decision;
        } catch (Exception e) {
            logger.error("Error in ML model prediction", e);
            // Fallback decision in case of error
            return getFallbackDecision();
        }
    }

    /**
     * Prepare input data for machine learning model
     *
     * @param loanApplication Loan application to convert
     * @return Map of input parameters for ML model
     */
    private Map<String, Object> prepareMachineLearningInput(LoanApplication loanApplication) {
        Map<String, Object> input = new HashMap<>();

        // Personal and Financial Information
        input.put("age", calculateAge(loanApplication));
        input.put("province", loanApplication.getPersonalInfo().getAddress().getState());

        // Employment Details
        input.put("employment_status",
                loanApplication.getFinancialInfo().getEmployment().getEmploymentType());
        input.put("months_employed",
                loanApplication.getFinancialInfo().getEmployment().getEmploymentDuration() * 12);

        // Financial Metrics
        input.put("annual_income",
                loanApplication.getFinancialInfo().getMonthlyIncome().multiply(BigDecimal.valueOf(12)));
        input.put("monthly_expenses",
                loanApplication.getFinancialInfo().getMonthlyExpenses());
        input.put("self_reported_expenses",
                loanApplication.getFinancialInfo().getMonthlyExpenses());

        // Loan Details
        input.put("requested_amount",
                loanApplication.getLoanDetails().getRequestedAmount());

        // Credit Information
        input.put("credit_score",
                loanApplication.getFinancialInfo().getCreditScore());
        input.put("credit_utilization", calculateCreditUtilization(loanApplication));
        input.put("num_open_accounts", countOpenAccounts(loanApplication));
        input.put("num_credit_inquiries", countCreditInquiries(loanApplication));
        input.put("payment_history", "On Time"); // Placeholder
        input.put("current_credit_limit", calculateCreditLimit(loanApplication));
        input.put("self_reported_debt", calculateTotalDebt(loanApplication));
        input.put("estimated_debt", calculateTotalDebt(loanApplication));

        return input;
    }

    /**
     * Call external ML model endpoint
     *
     * @param input Prepared input for ML model
     * @return Loan decision
     */
    private LoanDecision callMLModelEndpoint(Map<String, Object> input) {
        try {
            // Call external ML model
            Map<String, Object> response = restTemplate.postForObject(
                    mlModelEndpoint,
                    input,
                    Map.class
            );

            // Process response
            return processModelResponse(response);
        } catch (Exception e) {
            logger.error("Error calling ML model endpoint", e);
            return getFallbackDecision();
        }
    }

    /**
     * Process response from ML model
     *
     * @param response Response from ML model
     * @return Loan decision
     */
    private LoanDecision processModelResponse(Map<String, Object> response) {
        if (response == null) {
            logger.warn("Received null response from ML model");
            return getFallbackDecision();
        }

        // Extract approval status and probability
        Boolean approved = (Boolean) response.get("approved");
        Double probability = ((Number) response.get("probability")).doubleValue();

        return new LoanDecision(
                approved != null ? approved : false,
                probability != null ? probability : 0.0
        );
    }

    /**
     * Provide fallback decision in case of model failure
     *
     * @return Default loan decision
     */
    private LoanDecision getFallbackDecision() {
        logger.warn("Using fallback loan decision");
        return new LoanDecision(false, 0.0);
    }

    // Utility methods for calculations
    private int calculateAge(LoanApplication loanApplication) {
        return java.time.Period.between(
                loanApplication.getPersonalInfo().getDateOfBirth(),
                java.time.LocalDate.now()
        ).getYears();
    }

    private double calculateCreditUtilization(LoanApplication loanApplication) {
        // Placeholder implementation
        return 30.0; // Default 30% utilization
    }

    private int countOpenAccounts(LoanApplication loanApplication) {
        // Placeholder implementation
        return 3; // Default 3 open accounts
    }

    private int countCreditInquiries(LoanApplication loanApplication) {
        // Placeholder implementation
        return 2; // Default 2 credit inquiries
    }

    private BigDecimal calculateCreditLimit(LoanApplication loanApplication) {
        // Placeholder implementation
        return BigDecimal.valueOf(20000);
    }

    private BigDecimal calculateTotalDebt(LoanApplication loanApplication) {
        return loanApplication.getFinancialInfo().getDebts().stream()
                .map(debt -> debt.getOutstandingAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Logging methods
    private void logModelInput(Map<String, Object> input) {
        logger.info("ML Model Input: {}", input);
    }

    private void logModelDecision(LoanDecision decision) {
        logger.info(
                "ML Model Decision - Approved: {}, Probability: {}",
                decision.isApproved(),
                decision.getApprovalProbability()
        );
    }
}