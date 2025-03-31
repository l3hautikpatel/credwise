package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.repository.LoanApplicationRepository;
import com.team1_5.credwise.service.LoanApplicationResultService;
import com.team1_5.credwise.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoanApplicationResultController {

    private static final Logger logger = LoggerFactory.getLogger(LoanApplicationResultController.class);
    
    private final LoanApplicationResultService loanApplicationResultService;
    private final LoanApplicationRepository loanApplicationRepository;
    private final JwtUtil jwtUtil;

    public LoanApplicationResultController(
            LoanApplicationResultService loanApplicationResultService, 
            LoanApplicationRepository loanApplicationRepository,
            JwtUtil jwtUtil) {
        this.loanApplicationResultService = loanApplicationResultService;
        this.loanApplicationRepository = loanApplicationRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/admin/loan-application-results/{loanApplicationId}")
    public ResponseEntity<?> getLoanApplicationResultForAdmin(@PathVariable Long loanApplicationId) {
        try {
            LoanApplicationResultResponse result = loanApplicationResultService.getLoanApplicationResult(loanApplicationId);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            logger.error("Loan application result not found: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not Found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error retrieving loan application result for admin", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "An unexpected error occurred while processing your request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Endpoint for users to view their own loan application results
     * Authenticated users can only access their own loan application results
     */
    @GetMapping("/loan-applications-results/{applicationId}")
    public ResponseEntity<?> getLoanApplicationResult(
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String token) {
        
        try {
            // Extract user ID from JWT token
            Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            
            logger.info("Retrieving loan application result for application ID: {} by user ID: {}", 
                       applicationId, userId);
            
            // First verify that the loan application belongs to the user
            LoanApplication application = loanApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Loan application not found with id: " + applicationId));
            
            // Verify user ownership
            if (!application.getUser().getId().equals(userId)) {
                logger.warn("User {} attempted to access loan application {} that belongs to user {}", 
                           userId, applicationId, application.getUser().getId());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Unauthorized");
                errorResponse.put("message", "You are not authorized to access this loan application result");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }
            
            // Get the result
            LoanApplicationResultResponse result = loanApplicationResultService.getLoanApplicationResult(applicationId);
            return ResponseEntity.ok(result);
            
        } catch (ResourceNotFoundException e) {
            logger.error("Loan application or result not found: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not Found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error retrieving loan application result", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "An unexpected error occurred while processing your request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/admin/loan-application-results/generate/{loanApplicationId}")
    public ResponseEntity<?> generateLoanApplicationResult(@PathVariable Long loanApplicationId) {
        try {
            LoanApplicationResultResponse result = loanApplicationResultService.generateLoanApplicationResult(loanApplicationId);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            logger.error("Loan application not found: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Not Found");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error generating loan application result", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "An unexpected error occurred while processing your request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
