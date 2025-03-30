package com.team1_5.credwise.service;

import com.team1_5.credwise.model.FinancialInfo;
import com.team1_5.credwise.model.EmploymentHistory;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.PersonalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LoanMLService {
    private static final Logger logger = LoggerFactory.getLogger(LoanMLService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${loan.ml.api.url:http://localhost:8000/predict}")
    private String mlApiUrl;
    
    public LoanMLService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Call the ML model API to get loan application decision
     * 
     * @param application The loan application
     * @param financialInfo Financial information
     * @param personalInfo Personal information
     * @return Map containing the API response with decision factors
     */
    public Map<String, Object> getLoanDecision(LoanApplication application, FinancialInfo financialInfo, PersonalInfo personalInfo) {
        try {
            logger.info("Starting ML decision process for application ID: {}", application.getId());
            
            // Prepare request data for ML API
            Map<String, Object> requestData = prepareMLRequestData(application, financialInfo, personalInfo);
            
            logger.info("Prepared ML request data for application {}: {}", application.getId(), requestData);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with headers and body
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);
            
            // Log the API URL
            logger.info("Calling ML API at URL: {}", mlApiUrl);
            
            // Call ML API
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(mlApiUrl, entity, Map.class);
                
                // Process response
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> responseData = new HashMap<>(response.getBody());
                    logger.info("ML API response for application {}: {}", application.getId(), responseData);
                    return responseData;
                } else {
                    logger.error("ML API error for application {}: {}", application.getId(), response.getStatusCode());
                    return createErrorResponse("ML API error: " + response.getStatusCode());
                }
            } catch (Exception e) {
                logger.error("Error during ML API call for application {}: {}", application.getId(), e.getMessage(), e);
                
                // Instead of failing, return a default fallback evaluation
                // This allows processing to continue even if the ML service is unavailable
                logger.info("Using fallback credit evaluation for application {}", application.getId());
                return createFallbackEvaluation(application, financialInfo);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error in ML processing for application {}: {}", 
                    application != null ? application.getId() : "null", e.getMessage(), e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Apply ML model decision to the loan application
     * 
     * @param application The loan application
     * @param mlDecision ML model decision data
     * @return Updated loan application with decision applied
     */
    public LoanApplication applyMLDecision(LoanApplication application, Map<String, Object> mlDecision) {
        if (mlDecision.containsKey("error")) {
            logger.warn("Error in ML decision for application {}: {}", 
                    application.getId(), mlDecision.get("error"));
            application.setStatus("REVIEW_NEEDED");
            return application;
        }
        
        // Extract decision data - first try the direct ML model response
        boolean isApproved = false;
        
        if (mlDecision.containsKey("is_approved")) {
            isApproved = (boolean) mlDecision.get("is_approved");
        } else if (mlDecision.containsKey("approval_probability")) {
            // Use approval probability threshold if direct approval not available
            double approvalProbability = ((Number) mlDecision.get("approval_probability")).doubleValue();
            isApproved = approvalProbability >= 0.7; // 70% threshold
        } else {
            // Use credit score as a fallback
            Integer creditScore = null;
            if (mlDecision.containsKey("creditScore")) {
                creditScore = ((Number) mlDecision.get("creditScore")).intValue();
                
                // Basic credit score threshold: 660 is common minimum for approval
                isApproved = creditScore >= 660;
            } else {
                // Can't determine approval without any decision metrics
                logger.warn("No decision metrics (is_approved, approval_probability, or creditScore) found for application {}", 
                        application.getId());
                application.setStatus("REVIEW_NEEDED");
                return application;
            }
        }
        
        // Update application status
        application.setStatus(isApproved ? "APPROVED" : "DENIED");
        logger.info("Application {} status set to {}", application.getId(), application.getStatus());
        
        // Set ML-predicted credit score if available
        if (mlDecision.containsKey("predicted_credit_score")) {
            Double predictedScore = ((Number) mlDecision.get("predicted_credit_score")).doubleValue();
            application.setCreditScore(predictedScore);
            logger.info("Set ML-predicted credit score {} for application {}", predictedScore, application.getId());
        } else if (mlDecision.containsKey("creditScore")) {
            Double calculatedScore = ((Number) mlDecision.get("creditScore")).doubleValue();
            application.setCreditScore(calculatedScore);
            logger.info("Set calculated credit score {} for application {}", calculatedScore, application.getId());
        }
        
        return application;
    }
    
    /**
     * Prepare request data for ML API
     */
    private Map<String, Object> prepareMLRequestData(LoanApplication application, FinancialInfo financialInfo, PersonalInfo personalInfo) {
        Map<String, Object> requestData = new HashMap<>();
        
        // Extract personal information
        int age = 0;
        if (personalInfo != null && personalInfo.getDateOfBirth() != null) {
            age = java.time.Period.between(personalInfo.getDateOfBirth(), java.time.LocalDate.now()).getYears();
        }
        
        String province = "";
        if (personalInfo != null && personalInfo.getAddress() != null) {
            province = personalInfo.getAddress().getProvince();
        }
        
        // Extract employment information
        String employmentStatus = "Unemployed";
        int monthsEmployed = 0;
        
        if (financialInfo != null && financialInfo.getEmploymentDetails() != null) {
            List<EmploymentHistory> employments = financialInfo.getEmploymentDetails();
            
            if (!employments.isEmpty()) {
                // Get the most recent employment
                Optional<EmploymentHistory> currentEmployment = employments.stream()
                        .filter(e -> e.getEndDate() == null)
                        .findFirst();
                
                if (currentEmployment.isPresent()) {
                    employmentStatus = currentEmployment.get().getEmploymentType();
                } else {
                    employmentStatus = employments.get(0).getEmploymentType();
                }
                
                // Sum up months employed across all jobs
                monthsEmployed = employments.stream()
                        .mapToInt(EmploymentHistory::getDurationMonths)
                        .sum();
            }
        }
        
        // Extract financial information
        double annualIncome = 0.0;
        if (financialInfo != null && financialInfo.getMonthlyIncome() != null) {
            // Convert monthly to annual
            annualIncome = financialInfo.getMonthlyIncome().multiply(BigDecimal.valueOf(12)).doubleValue();
        }
        
        double selfReportedDebt = 0.0;
        if (financialInfo != null && financialInfo.getEstimatedDebts() != null) {
            selfReportedDebt = financialInfo.getEstimatedDebts().doubleValue();
        }
        
        double selfReportedExpenses = 0.0;
        if (financialInfo != null && financialInfo.getMonthlyExpenses() != null) {
            selfReportedExpenses = financialInfo.getMonthlyExpenses().doubleValue();
        }
        
        double totalCreditLimit = 0.0;
        if (financialInfo != null && financialInfo.getCurrentCreditLimit() != null) {
            totalCreditLimit = financialInfo.getCurrentCreditLimit().doubleValue();
        }
        
        double creditUtilization = 0.0;
        if (financialInfo != null && financialInfo.getCreditUtilization() != null) {
            creditUtilization = financialInfo.getCreditUtilization().doubleValue();
        }
        
        // Number of open accounts - use debt types count as a proxy
        int numOpenAccounts = 0;
        if (financialInfo != null && financialInfo.getExistingDebts() != null) {
            numOpenAccounts = financialInfo.getExistingDebts().size();
        }
        
        // Default to 0 credit inquiries
        int numCreditInquiries = 0;
        
        // Monthly expenses - use from financial info
        double monthlyExpenses = 0.0;
        if (financialInfo != null && financialInfo.getMonthlyExpenses() != null) {
            monthlyExpenses = financialInfo.getMonthlyExpenses().doubleValue();
        }
        
        // DTI calculation
        double dti = 0.0;
        if (financialInfo != null && financialInfo.getDebtToIncomeRatio() != null) {
            dti = financialInfo.getDebtToIncomeRatio().multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        
        // Payment history - analyze from debts
        String paymentHistory = "On Time";
        if (application.getCreditEvaluationData() != null && application.getCreditEvaluationData().containsKey("paymentHistoryRating")) {
            String rating = (String) application.getCreditEvaluationData().get("paymentHistoryRating");
            if ("Fair".equals(rating)) {
                paymentHistory = "Late < 30";
            } else if ("Poor".equals(rating)) {
                paymentHistory = "Late > 30";
            }
        }
        
        // Requested amount
        double requestedAmount = 0.0;
        if (application.getRequestedAmount() != null) {
            requestedAmount = application.getRequestedAmount().doubleValue();
        }
        
        // Estimated debt - use from financial info
        double estimatedDebt = 0.0;
        if (financialInfo != null && financialInfo.getEstimatedDebts() != null) {
            estimatedDebt = financialInfo.getEstimatedDebts().doubleValue();
        }
        
        // Populate the request data map
        requestData.put("age", age);
        requestData.put("province", province);
        requestData.put("employment_status", employmentStatus);
        requestData.put("months_employed", monthsEmployed);
        requestData.put("annual_income", annualIncome);
        requestData.put("self_reported_debt", selfReportedDebt);
        requestData.put("self_reported_expenses", selfReportedExpenses);
        requestData.put("total_credit_limit", totalCreditLimit);
        requestData.put("credit_utilization", creditUtilization);
        requestData.put("num_open_accounts", numOpenAccounts);
        requestData.put("num_credit_inquiries", numCreditInquiries);
        requestData.put("monthly_expenses", monthlyExpenses);
        requestData.put("dti", dti);
        requestData.put("payment_history", paymentHistory);
        requestData.put("requested_amount", requestedAmount);
        requestData.put("estimated_debt", estimatedDebt);
        
        return requestData;
    }
    
    /**
     * Create error response map
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        errorResponse.put("is_approved", false);
        return errorResponse;
    }
    
    /**
     * Create a fallback evaluation when the ML API is unavailable
     */
    private Map<String, Object> createFallbackEvaluation(LoanApplication application, FinancialInfo financialInfo) {
        Map<String, Object> fallback = new HashMap<>();
        
        // Use the credit score from financial info as the basis for decision
        Integer creditScore = financialInfo.getSystemCreditScore();
        if (creditScore == null && financialInfo.getCreditScore() != null) {
            creditScore = financialInfo.getCreditScore();
        }
        if (creditScore == null) {
            creditScore = 650; // Default score if none available
        }
        
        // Determine approval based on credit score
        boolean isApproved = creditScore >= 660;
        
        // Set basic fields
        fallback.put("fallback", true);
        fallback.put("is_approved", isApproved);
        fallback.put("approval_probability", isApproved ? 0.85 : 0.3);
        fallback.put("approved_amount", isApproved ? application.getRequestedAmount().doubleValue() : 0.0);
        
        // Calculate interest rate based on credit score (simplified version)
        double baseRate = 0.05; // 5%
        if (creditScore < 660) baseRate += 0.02;
        else if (creditScore < 720) baseRate += 0.01;
        
        fallback.put("interest_rate", baseRate);
        fallback.put("creditScore", creditScore);
        
        logger.info("Created fallback evaluation for application {}: {}", application.getId(), fallback);
        return fallback;
    }
} 