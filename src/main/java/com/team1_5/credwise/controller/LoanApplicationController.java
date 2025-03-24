package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanApplicationResponse;
import com.team1_5.credwise.service.LoanApplicationService;
import com.team1_5.credwise.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService loanService;
    private final JwtUtil jwtUtil;

    public LoanApplicationController(LoanApplicationService loanService, JwtUtil jwtUtil) {
        this.loanService = loanService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> submitApplication(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody LoanApplicationRequest request) {

        Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        LoanApplicationResponse response = loanService.processLoanApplication(userId, request);
        return ResponseEntity.ok(response);
    }
}