package com.team1_5.credwise.controller;

import com.team1_5.credwise.dto.FinancialSummaryResponse;
import com.team1_5.credwise.service.FinancialSummaryService;
import com.team1_5.credwise.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/financial-summary")
public class FinancialSummaryController {

    private final FinancialSummaryService financialSummaryService;
    private final JwtUtil jwtUtil;

    public FinancialSummaryController(FinancialSummaryService financialSummaryService, JwtUtil jwtUtil) {
        this.financialSummaryService = financialSummaryService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getFinancialSummary(@RequestHeader("Authorization") String token) {
        try {
            // Extract user ID from JWT token
            Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            FinancialSummaryResponse response = financialSummaryService.getFinancialSummary(userId);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        }
    }
}
