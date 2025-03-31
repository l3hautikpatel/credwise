package com.team1_5.credwise.controller;

import com.team1_5.credwise.model.FinancialInfo;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.PersonalInfo;
import com.team1_5.credwise.service.LoanApplicationService;
import com.team1_5.credwise.service.LoanMLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for batch processing loan applications
 */
@RestController
@RequestMapping("/api/loan-applications/batch")
public class LoanBatchProcessingController {
    
    private static final Logger logger = LoggerFactory.getLogger(LoanBatchProcessingController.class);
    
    private final LoanApplicationService loanApplicationService;
    private final LoanMLService loanMLService;
    
    public LoanBatchProcessingController(
            LoanApplicationService loanApplicationService,
            LoanMLService loanMLService) {
        this.loanApplicationService = loanApplicationService;
        this.loanMLService = loanMLService;
    }

    /**
     * Process all submitted loan applications
     * 
     * @return Response with the number of applications processed
     */
    @PostMapping("/process-submitted")
    public ResponseEntity<Map<String, Object>> processAllSubmittedApplications() {
        try {
            logger.info("Starting batch processing of submitted loan applications");
            int processedCount = loanApplicationService.processAllSubmittedApplications(loanMLService);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("processedCount", processedCount);
            response.put("message", "Processed " + processedCount + " loan applications");
            
            logger.info("Completed batch processing, processed {} applications", processedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in batch processing of loan applications: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Error processing applications: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Process a specific loan application by ID
     * 
     * @param applicationId The ID of the loan application to process
     * @return Response with details of the processed application
     */
    @PostMapping("/process-application/{applicationId}")
    public ResponseEntity<Map<String, Object>> processLoanApplication(@PathVariable Long applicationId) {
        try {
            logger.info("Starting processing of loan application ID: {}", applicationId);
            
            // Get the application
            LoanApplication application = loanApplicationService.getLoanApplication(applicationId);
            
            if (application == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Loan application not found with ID: " + applicationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            logger.info("Found loan application with status: {}", application.getStatus());
            
            // Get personal and financial info
            PersonalInfo personalInfo = loanApplicationService.getPersonalInfo(applicationId);
            FinancialInfo financialInfo = loanApplicationService.getFinancialInfo(applicationId);
            
            if (personalInfo == null || financialInfo == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Personal or financial information missing for application: " + applicationId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Process application
            application = loanApplicationService.processApplicationWithML(applicationId, personalInfo, financialInfo, loanMLService);
            
            logger.info("Processed application, new status: {}", application.getStatus());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicationId", applicationId);
            response.put("status", application.getStatus());
            response.put("systemCreditScore", financialInfo.getSystemCreditScore());
            response.put("eligibilityScore", financialInfo.getEligibilityScore());
            response.put("message", "Application processed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing loan application {}: {}", applicationId, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Error processing application: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 