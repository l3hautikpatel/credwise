package com.team1_5.credwise.config;


import java.math.BigDecimal;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Global Application Configuration
 *
 * Centralizes application-wide configuration parameters
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Configuration
@PropertySource("classpath:application-config.properties")
public class ApplicationConfig {

    // Loan Application Configurations
    @Value("${loan.max.applications:1000000}")
    private int maxApplicationsPerUser;

    @Value("${loan.cooldown.period.months:1}")
    private int loanApplicationCooldownPeriod;

    // Credit Score Configurations
    @Value("${credit.score.minimum:600}")
    private int minimumCreditScore;

    @Value("${credit.score.excellent.min:750}")
    private int excellentCreditScoreMin;

    @Value("${credit.score.good.min:700}")
    private int goodCreditScoreMin;

    @Value("${credit.score.good.max:749}")
    private int goodCreditScoreMax;

    @Value("${credit.score.fair.min:650}")
    private int fairCreditScoreMin;

    @Value("${credit.score.fair.max:699}")
    private int fairCreditScoreMax;

    // Loan Amount Configurations
    @Value("${loan.amount.minimum:1000}")
    private BigDecimal minimumLoanAmount;

    @Value("${loan.amount.maximum:1000000}")
    private BigDecimal maximumLoanAmount;

    // Loan Term Configurations
    @Value("${loan.term.minimum:1}")
    private int minimumLoanTermMonths;

    @Value("${loan.term.maximum:360}")
    private int maximumLoanTermMonths;

    // Machine Learning Model Configurations
    @Value("${ml.model.endpoint:http://localhost:5000/predict}")
    private String mlModelEndpoint;

    @Value("${ml.model.timeout:5000}")
    private int mlModelTimeout;

    // Debt-to-Income Ratio Configurations
    @Value("${debt.income.ratio.maximum:0.36}")
    private BigDecimal maximumDebtToIncomeRatio;

    // Logging and Audit Configurations
    @Value("${audit.log.enabled:true}")
    private boolean auditLogEnabled;

    @Value("${audit.log.retention.days:90}")
    private int auditLogRetentionDays;

    // Getters for all configurations
    public int getMaxApplicationsPerUser() {
        return maxApplicationsPerUser;
    }

    public int getLoanApplicationCooldownPeriod() {
        return loanApplicationCooldownPeriod;
    }

    public int getMinimumCreditScore() {
        return minimumCreditScore;
    }

    public int getExcellentCreditScoreMin() {
        return excellentCreditScoreMin;
    }

    public int getGoodCreditScoreMin() {
        return goodCreditScoreMin;
    }

    public int getGoodCreditScoreMax() {
        return goodCreditScoreMax;
    }

    public int getFairCreditScoreMin() {
        return fairCreditScoreMin;
    }

    public int getFairCreditScoreMax() {
        return fairCreditScoreMax;
    }

    public BigDecimal getMinimumLoanAmount() {
        return minimumLoanAmount;
    }

    public BigDecimal getMaximumLoanAmount() {
        return maximumLoanAmount;
    }

    public int getMinimumLoanTermMonths() {
        return minimumLoanTermMonths;
    }

    public int getMaximumLoanTermMonths() {
        return maximumLoanTermMonths;
    }

    public String getMlModelEndpoint() {
        return mlModelEndpoint;
    }

    public int getMlModelTimeout() {
        return mlModelTimeout;
    }

    public BigDecimal getMaximumDebtToIncomeRatio() {
        return maximumDebtToIncomeRatio;
    }

    public boolean isAuditLogEnabled() {
        return auditLogEnabled;
    }

    public int getAuditLogRetentionDays() {
        return auditLogRetentionDays;
    }

    /**
     * Validate configuration on bean initialization
     */
    @PostConstruct
    public void validateConfiguration() {
        validateLoanConfigurations();
        validateCreditScoreConfigurations();
        validateMlModelConfigurations();
    }

    /**
     * Validate loan-related configurations
     */
    private void validateLoanConfigurations() {
        if (minimumLoanAmount.compareTo(maximumLoanAmount) > 0) {
            throw new IllegalStateException("Minimum loan amount cannot be greater than maximum loan amount");
        }

        if (minimumLoanTermMonths > maximumLoanTermMonths) {
            throw new IllegalStateException("Minimum loan term cannot be greater than maximum loan term");
        }
    }

    /**
     * Validate credit score configurations
     */
    private void validateCreditScoreConfigurations() {
        if (minimumCreditScore < 300 || minimumCreditScore > 850) {
            throw new IllegalStateException("Invalid minimum credit score");
        }

        if (excellentCreditScoreMin < minimumCreditScore) {
            throw new IllegalStateException("Excellent credit score minimum cannot be less than minimum credit score");
        }
    }

    /**
     * Validate machine learning model configurations
     */
    private void validateMlModelConfigurations() {
        if (mlModelTimeout < 0) {
            throw new IllegalStateException("ML model timeout cannot be negative");
        }
    }

    /**
     * Create a builder for dynamic configuration
     */
    public static class Builder {
        private ApplicationConfig config = new ApplicationConfig();

        public Builder maxApplicationsPerUser(int maxApplications) {
            config.maxApplicationsPerUser = maxApplications;
            return this;
        }

        public Builder loanApplicationCooldownPeriod(int cooldownPeriod) {
            config.loanApplicationCooldownPeriod = cooldownPeriod;
            return this;
        }

        // Add more builder methods for other configurations

        public ApplicationConfig build() {
            config.validateConfiguration();
            return config;
        }
    }

    /**
     * Static method to create a configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }
}