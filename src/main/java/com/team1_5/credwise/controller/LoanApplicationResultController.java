package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.LoanApplicationResult;
import com.team1_5.credwise.service.LoanApplicationResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationResultController {
    
    private static final Logger logger = LoggerFactory.getLogger(LoanApplicationResultController.class);
    private final LoanApplicationResultService loanApplicationResultService;

    public LoanApplicationResultController(LoanApplicationResultService loanApplicationResultService) {
        this.loanApplicationResultService = loanApplicationResultService;
    }

    /**
     * Get the result of a loan application
     *
     * @param applicationId The loan application ID
     * @return The loan application result
     */
    @GetMapping("/{applicationId}/result")
    public ResponseEntity<LoanApplicationResultResponse> getLoanApplicationResult(@PathVariable Long applicationId) {
        try {
            LoanApplicationResultResponse response = loanApplicationResultService.getLoanApplicationResult(applicationId);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error getting loan application result for application {}: {}", 
                    applicationId, e.getMessage(), e);
            throw new RuntimeException("Error getting loan application result: " + e.getMessage());
        }
    }
    
    /**
     * Generate the result for a loan application
     *
     * @param applicationId The loan application ID
     * @return Success/failure response
     */
    @PostMapping("/{applicationId}/generate-result")
    public ResponseEntity<Map<String, Object>> generateLoanApplicationResult(@PathVariable Long applicationId) {
        try {
            logger.info("Generating loan application result for application ID: {}", applicationId);
            LoanApplicationResult result = loanApplicationResultService.generateLoanApplicationResult(applicationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Loan application result generated successfully");
            response.put("resultId", result.getId());
            response.put("status", result.getStatus());
            response.put("eligibilityScore", result.getEligibilityScore());
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error generating loan application result for application {}: {}", 
                    applicationId, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error generating loan application result: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
