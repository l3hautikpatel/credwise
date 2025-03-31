package com.team1_5.credwise.service;

import com.team1_5.credwise.model.FinancialInfo;
import com.team1_5.credwise.model.EmploymentHistory;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.PersonalInfo;
import com.team1_5.credwise.model.Debt;
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
import java.time.LocalDate;
import java.util.Comparator;
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
                    // Return error response - do not use fallback
                    return createErrorResponse("ML API error: " + response.getStatusCode());
                }
            } catch (Exception e) {
                logger.error("Error during ML API call for application {}: {}", application.getId(), e.getMessage(), e);
                
                // Do not use fallback evaluation - let the caller handle the failure properly
                return createErrorResponse("ML API unavailable: " + e.getMessage());
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
            // Set status to REVIEW_NEEDED when ML API fails
            application.setStatus("REVIEW_NEEDED");
            return application;
        }
        
        // Strictly follow the ML API decision
        boolean isApproved = false;
        
        // Check for is_approved flag (primary decision point)
        if (mlDecision.containsKey("is_approved")) {
            isApproved = Boolean.TRUE.equals(mlDecision.get("is_approved"));
            logger.info("ML decision based on is_approved = {}", isApproved);
            
            // Set status based directly on ML API response
            application.setStatus(isApproved ? "APPROVED" : "DENIED");
            logger.info("Application {} status set to {} based on direct ML decision", 
                    application.getId(), application.getStatus());
        } 
        // Only use approval_probability if is_approved is not present
        else if (mlDecision.containsKey("approval_probability")) {
            double approvalProbability = ((Number) mlDecision.get("approval_probability")).doubleValue();
            isApproved = approvalProbability >= 0.7; // 70% threshold
            logger.info("ML decision based on approval_probability = {}, isApproved = {}", 
                    approvalProbability, isApproved);
            
            // Set status based on probability threshold
            application.setStatus(isApproved ? "APPROVED" : "DENIED");
            logger.info("Application {} status set to {} based on approval probability", 
                    application.getId(), application.getStatus());
        } 
        else {
            // No approval information available - require manual review
            logger.warn("No approval decision metrics in ML response for application {}", 
                    application.getId());
            application.setStatus("REVIEW_NEEDED");
            return application;
        }
        
        // Set ML-predicted credit score if available
        if (mlDecision.containsKey("predicted_credit_score")) {
            Double predictedScore = ((Number) mlDecision.get("predicted_credit_score")).doubleValue();
            application.setCreditScore(predictedScore);
            logger.info("Set ML-predicted credit score {} for application {}", predictedScore, application.getId());
        }
        
        // Get existing credit evaluation data or create new map
        Map<String, Object> creditEvaluationData = application.getCreditEvaluationData();
        if (creditEvaluationData == null) {
            creditEvaluationData = new HashMap<>();
        }
        
        // Store all ML decision data in credit evaluation data
        for (Map.Entry<String, Object> entry : mlDecision.entrySet()) {
            creditEvaluationData.put(entry.getKey(), entry.getValue());
        }
        
        // Format approved amount
        if (mlDecision.containsKey("approved_amount")) {
            creditEvaluationData.put("approved_amount", mlDecision.get("approved_amount"));
        } else {
            // Set approved amount based on ML approval decision
            creditEvaluationData.put("approved_amount", isApproved ? 
                application.getRequestedAmount().doubleValue() : 0.0);
        }
        
        // Format interest rate if available
        if (mlDecision.containsKey("interest_rate") && mlDecision.get("interest_rate") != null) {
            Double interestRate = ((Number) mlDecision.get("interest_rate")).doubleValue();
            creditEvaluationData.put("interest_rate", interestRate);
            // Add formatted interest rate string for display
            creditEvaluationData.put("interest_rate_formatted", String.format("%.2f%%", interestRate * 100));
        }
        
        // Update the application with the ML data
        application.setCreditEvaluationData(creditEvaluationData);
        
        return application;
    }
    
    /**
     * Prepare request data for ML API
     */
    private Map<String, Object> prepareMLRequestData(LoanApplication application, FinancialInfo financialInfo, PersonalInfo personalInfo) {
        Map<String, Object> requestData = new HashMap<>();
        
        try {
            // Log the input data for debugging purposes
            logger.info("Preparing ML request data for application ID: {}", application.getId());
            
            // Extract personal information
            int age = 0;
            if (personalInfo != null && personalInfo.getDateOfBirth() != null) {
                age = java.time.Period.between(personalInfo.getDateOfBirth(), java.time.LocalDate.now()).getYears();
                logger.info("Calculated age: {} from DOB: {}", age, personalInfo.getDateOfBirth());
            }
            
            String province = "";
            if (personalInfo != null && personalInfo.getAddress() != null) {
                province = personalInfo.getAddress().getProvince();
                logger.info("Province from address: {}", province);
            }
            
            // Extract employment information
            String employmentStatus = "Unemployed";
            int monthsEmployed = 0;
            
            if (financialInfo != null && financialInfo.getEmploymentDetails() != null) {
                List<EmploymentHistory> employments = financialInfo.getEmploymentDetails();
                
                if (!employments.isEmpty()) {
                    // Debug all employment records
                    logger.info("Found {} employment records", employments.size());
                    for (EmploymentHistory emp : employments) {
                        logger.info("Employment record: {} at {}, type: {}, duration: {} months", 
                                  emp.getPosition(), emp.getEmployerName(), 
                                  emp.getEmploymentType(), emp.getDurationMonths());
                    }
                    
                    // Get the most recent employment
                    Optional<EmploymentHistory> currentEmployment = employments.stream()
                            .filter(e -> e.getEndDate() == null)
                            .findFirst();
                    
                    EmploymentHistory employment = currentEmployment.orElse(employments.get(0));
                    
                    // Get employment type
                    employmentStatus = employment.getEmploymentType();
                    if (employmentStatus == null || employmentStatus.isEmpty()) {
                        employmentStatus = "Unemployed";
                    }
                    
                    // Get employment duration
                    if (employment.getDurationMonths() != null && employment.getDurationMonths() > 0) {
                        monthsEmployed = employment.getDurationMonths();
                    } else if (employment.getStartDate() != null) {
                        // Calculate from dates if duration is not directly available
                        LocalDate endDate = employment.getEndDate() != null ? 
                                          employment.getEndDate() : LocalDate.now();
                        monthsEmployed = java.time.Period.between(
                            employment.getStartDate(), endDate).getYears() * 12 + 
                            java.time.Period.between(employment.getStartDate(), endDate).getMonths();
                    }
                    
                    // Log employment calculation for debugging
                    logger.info("Using employment status: {}, Months employed: {}", employmentStatus, monthsEmployed);
                }
            }
            
            // Extract financial information
            double annualIncome = 0.0;
            if (financialInfo != null && financialInfo.getMonthlyIncome() != null) {
                // Convert monthly to annual
                annualIncome = financialInfo.getMonthlyIncome().multiply(BigDecimal.valueOf(12)).doubleValue();
                logger.info("Annual income calculated: {} from monthly income: {}", 
                          annualIncome, financialInfo.getMonthlyIncome());
            }
            
            double selfReportedDebt = 0.0;
            if (financialInfo != null && financialInfo.getEstimatedDebts() != null) {
                selfReportedDebt = financialInfo.getEstimatedDebts().doubleValue();
                logger.info("Self-reported debt: {}", selfReportedDebt);
            }
            
            double selfReportedExpenses = 0.0;
            if (financialInfo != null && financialInfo.getMonthlyExpenses() != null) {
                selfReportedExpenses = financialInfo.getMonthlyExpenses().doubleValue();
                logger.info("Self-reported expenses: {}", selfReportedExpenses);
            }
            
            double totalCreditLimit = 0.0;
            if (financialInfo != null && financialInfo.getCurrentCreditLimit() != null) {
                totalCreditLimit = financialInfo.getCurrentCreditLimit().doubleValue();
                logger.info("Total credit limit: {}", totalCreditLimit);
            }
            
            // Credit utilization calculation with proper handling of over-limit situations
            double creditUtilization = 0.0;
            if (financialInfo != null) {
                BigDecimal creditLimit = financialInfo.getCurrentCreditLimit();
                BigDecimal creditUsage = financialInfo.getCreditTotalUsage();
                
                if (creditLimit != null && creditUsage != null) {
                    if (creditLimit.compareTo(BigDecimal.ZERO) > 0) {
                        // Calculate utilization as a percentage
                        BigDecimal utilization = creditUsage.divide(creditLimit, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                        
                        creditUtilization = utilization.doubleValue();
                        
                        // Log warning if over 100%
                        if (creditUtilization > 100.0) {
                            logger.warn("Credit utilization is over 100%: {}% for application ID: {}", 
                                       creditUtilization, application.getId());
                        }
                        
                        logger.info("Credit utilization calculated: {}%", creditUtilization);
                    } else {
                        logger.warn("Credit limit is zero, cannot calculate utilization for application ID: {}", 
                                  application.getId());
                    }
                } else {
                    logger.warn("Missing credit limit or usage data for application ID: {}", application.getId());
                }
            }
            
            // Number of open accounts - use debt types count as a proxy
            int numOpenAccounts = 0;
            if (financialInfo != null && financialInfo.getExistingDebts() != null) {
                numOpenAccounts = financialInfo.getExistingDebts().size();
                logger.info("Number of open accounts: {}", numOpenAccounts);
            }
            
            // Default to 0 credit inquiries
            int numCreditInquiries = 0;
            
            // Monthly expenses - use from financial info
            double monthlyExpenses = 0.0;
            if (financialInfo != null && financialInfo.getMonthlyExpenses() != null) {
                monthlyExpenses = financialInfo.getMonthlyExpenses().doubleValue();
                logger.info("Monthly expenses: {}", monthlyExpenses);
            }
            
            // DTI calculation - use the one from financial info if available
            double dti = 0.0;
            if (financialInfo != null) {
                if (financialInfo.getDebtToIncomeRatio() != null) {
                    dti = financialInfo.getDebtToIncomeRatio().multiply(BigDecimal.valueOf(100)).doubleValue();
                } else if (financialInfo.getMonthlyIncome() != null && 
                          financialInfo.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
                    // Calculate it if not available but we have income and expenses
                    BigDecimal annualizedIncome = financialInfo.getMonthlyIncome().multiply(BigDecimal.valueOf(12));
                    BigDecimal totalDebt = financialInfo.getEstimatedDebts() != null ? 
                                          financialInfo.getEstimatedDebts() : BigDecimal.ZERO;
                    if (financialInfo.getTotalDebts() != null) {
                        totalDebt = totalDebt.add(financialInfo.getTotalDebts());
                    }
                    
                    BigDecimal loanImpact = application.getRequestedAmount().multiply(BigDecimal.valueOf(0.03));
                    BigDecimal monthlyExpense = financialInfo.getMonthlyExpenses() != null ? 
                                              financialInfo.getMonthlyExpenses() : BigDecimal.ZERO;
                    
                    BigDecimal numerator = monthlyExpense.add(totalDebt).add(loanImpact);
                    dti = numerator.divide(annualizedIncome, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
                }
                logger.info("DTI calculated: {}", dti);
            }
            
            // Extract payment history from debts
            String paymentHistory = "On Time"; // Default value - ML API format with space, not hyphen
            if (financialInfo != null && financialInfo.getExistingDebts() != null) {
                // Count late payments
                long latePayments = financialInfo.getExistingDebts().stream()
                        .filter(debt -> debt.getPaymentHistory() != null && !debt.getPaymentHistory().equals("On-time"))
                        .count();
                        
                // Format payment history according to ML API expectations
                // The ML API expects specific values: "On Time", "Late", "Default"
                if (latePayments > 0) {
                    // Find the most severe late payment
                    Optional<String> worstPaymentHistory = financialInfo.getExistingDebts().stream()
                            .map(Debt::getPaymentHistory)
                            .filter(ph -> ph != null && !ph.equals("On-time"))
                            .max(Comparator.naturalOrder());
                            
                    if (worstPaymentHistory.isPresent()) {
                        paymentHistory = standardizePaymentHistory(worstPaymentHistory.get());
                        logger.info("Using worst payment history: {}", paymentHistory);
                    }
                }
            }
            
            logger.info("Payment history for ML API: {}", paymentHistory);
            
            // Requested amount
            double requestedAmount = 0.0;
            if (application.getRequestedAmount() != null) {
                requestedAmount = application.getRequestedAmount().doubleValue();
                logger.info("Requested amount: {}", requestedAmount);
            }
            
            // Estimated debt - use from financial info
            double estimatedDebt = 0.0;
            if (financialInfo != null && financialInfo.getEstimatedDebts() != null) {
                estimatedDebt = financialInfo.getEstimatedDebts().doubleValue();
                logger.info("Estimated debt: {}", estimatedDebt);
            }
            
            // User-provided credit score - directly from financial info
            Integer creditScore = null;
            if (financialInfo != null) {
                if (financialInfo.getSystemCreditScore() != null) {
                    creditScore = financialInfo.getSystemCreditScore();
                    logger.info("Using system calculated credit score: {}", creditScore);
                } else if (financialInfo.getCreditScore() != null) {
                    creditScore = financialInfo.getCreditScore();
                    logger.info("Using user-provided credit score: {}", creditScore);
                }
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
            
            // Include credit score if available (for ML models that use it)
            if (creditScore != null) {
                requestData.put("credit_score", creditScore);
            }
            
            // Log final prepared ML request data for verification
            logger.info("Final ML request data: {}", requestData);
            
        } catch (Exception e) {
            logger.error("Error preparing ML request data: {}", e.getMessage(), e);
            // Don't create fallback data - let the API call fail properly
            throw new RuntimeException("Failed to prepare ML request data: " + e.getMessage(), e);
        }
        
        return requestData;
    }
    
    /**
     * Debug method to expose the request data preparation for logging/debugging
     * This method has no side effects and just returns what would be sent
     */
    public Map<String, Object> debugPrepareMLRequestData(LoanApplication application, FinancialInfo financialInfo, PersonalInfo personalInfo) {
        try {
            return prepareMLRequestData(application, financialInfo, personalInfo);
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to prepare ML data: " + e.getMessage());
            return errorMap;
        }
    }
    
    /**
     * Create error response map
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        // Don't include default approval values
        return errorResponse;
    }
    
    /**
     * Standardize payment history to match ML API expectations
     */
    private String standardizePaymentHistory(String rawPaymentHistory) {
        if (rawPaymentHistory == null) {
            return "On Time"; // ML API format
        }
        
        String normalized = rawPaymentHistory.trim().toLowerCase();
        
        // Convert to one of the expected formats for ML API: "On Time", "Late", "Default"
        if (normalized.contains("on time") || normalized.contains("on-time") || 
            normalized.equals("good") || normalized.equals("excellent")) {
            return "On Time"; // ML API format has a space, not hyphen
        } else if (normalized.contains("< 30") || normalized.contains("less than 30") || 
                   normalized.contains("under 30") || normalized.contains("1-29") ||
                   normalized.contains("30-60") || normalized.contains("30 to 60") || 
                   normalized.contains("between 30 and 60")) {
            return "Late"; // ML API format - all late payments map to "Late"
        } else if (normalized.contains("> 60") || normalized.contains("over 60") || 
                   normalized.contains("more than 60") || normalized.contains("90+") ||
                   normalized.contains("default") || normalized.contains("bankruptcy")) {
            return "Default"; // ML API format - severe late payments map to "Default"
        }
        
        // Default mapping for unrecognized formats
        logger.warn("Unrecognized payment history format: '{}', defaulting to 'Late'", rawPaymentHistory);
        return "Late"; // ML API format
    }
} 