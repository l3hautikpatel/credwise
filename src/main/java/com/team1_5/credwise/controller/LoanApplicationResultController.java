package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.service.LoanApplicationResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan-applications-results")
public class LoanApplicationResultController {

    private final LoanApplicationResultService loanApplicationResultService;

    public LoanApplicationResultController(LoanApplicationResultService loanApplicationResultService) {
        this.loanApplicationResultService = loanApplicationResultService;
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<LoanApplicationResultResponse> getLoanApplicationResult(@PathVariable Long applicationId) {
        LoanApplicationResultResponse response = loanApplicationResultService.getLoanApplicationResult(applicationId);
        return ResponseEntity.ok(response);
    }
}
