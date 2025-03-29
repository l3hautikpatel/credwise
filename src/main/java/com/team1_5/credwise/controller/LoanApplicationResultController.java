package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.service.LoanApplicationResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationResultController {

    private final LoanApplicationResultService loanApplicationResultService;

    public LoanApplicationResultController(LoanApplicationResultService loanApplicationResultService) {
        this.loanApplicationResultService = loanApplicationResultService;
    }

    @GetMapping("/{loanApplicationId}/result")
    public ResponseEntity<LoanApplicationResultResponse> getLoanApplicationResult(@PathVariable Long loanApplicationId) {
        LoanApplicationResultResponse response = loanApplicationResultService.getLoanApplicationResult(loanApplicationId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{loanApplicationId}/generate-result")
    public ResponseEntity<LoanApplicationResultResponse> generateLoanApplicationResult(@PathVariable Long loanApplicationId) {
        // Generate the result
        loanApplicationResultService.generateLoanApplicationResult(loanApplicationId);
        
        // Return the generated result
        LoanApplicationResultResponse response = loanApplicationResultService.getLoanApplicationResult(loanApplicationId);
        return ResponseEntity.ok(response);
    }
}
