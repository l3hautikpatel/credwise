package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.FinancialInfo;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.PersonalInfo;
import com.team1_5.credwise.repository.FinancialInfoRepository;
import com.team1_5.credwise.repository.LoanApplicationRepository;
import com.team1_5.credwise.repository.PersonalInfoRepository;
import com.team1_5.credwise.service.LoanMLService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/loan-applications")
public class LoanMLController {

    private final LoanMLService loanMLService;
    private final LoanApplicationRepository loanApplicationRepository;
    private final FinancialInfoRepository financialInfoRepository;
    private final PersonalInfoRepository personalInfoRepository;

    public LoanMLController(
            LoanMLService loanMLService,
            LoanApplicationRepository loanApplicationRepository,
            FinancialInfoRepository financialInfoRepository,
            PersonalInfoRepository personalInfoRepository) {
        this.loanMLService = loanMLService;
        this.loanApplicationRepository = loanApplicationRepository;
        this.financialInfoRepository = financialInfoRepository;
        this.personalInfoRepository = personalInfoRepository;
    }

    /**
     * Process a loan application through the ML model
     * 
     * @param applicationId The loan application ID
     * @return Response with the application status and decision details
     */
    @PostMapping("/{applicationId}/process-ml")
    public ResponseEntity<Map<String, Object>> processApplicationWithML(@PathVariable Long applicationId) {
        // Find application
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found: " + applicationId));
        
        // Get related financial info
        FinancialInfo financialInfo = financialInfoRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial info not found for application: " + applicationId));
        
        // Get related personal info
        PersonalInfo personalInfo = personalInfoRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal info not found for application: " + applicationId));
        
        // Call ML service
        Map<String, Object> mlDecision = loanMLService.getLoanDecision(application, financialInfo, personalInfo);
        
        // Apply decision to application
        application = loanMLService.applyMLDecision(application, mlDecision);
        
        // Save updated application
        application = loanApplicationRepository.save(application);
        
        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("applicationId", application.getId());
        response.put("status", application.getStatus());
        response.put("decision", mlDecision);
        
        return ResponseEntity.ok(response);
    }
} 