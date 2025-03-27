package com.team1_5.credwise.controller;

import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.service.LoanApplicationHistoryService;
import com.team1_5.credwise.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loan-applications-history")
public class LoanApplicationHistoryController {

    private final LoanApplicationHistoryService loanApplicationHistoryService;
    private final JwtUtil jwtUtil;

    public LoanApplicationHistoryController(LoanApplicationHistoryService loanApplicationHistoryService, JwtUtil jwtUtil) {
        this.loanApplicationHistoryService = loanApplicationHistoryService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getLoanApplicationHistory(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        List<LoanApplication> applications = loanApplicationHistoryService.getLoanApplicationHistoryByUserId(userId);

        if (applications.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                applications.stream().map(app -> new LoanApplicationHistoryResponse(
                        app.getId(),
                        app.getStatus(),
                        app.getRequestedAmount(),
                        app.getCreatedAt().toString()
                )).collect(Collectors.toList())
        );
    }

    // Inner DTO class for clean JSON response
    private record LoanApplicationHistoryResponse(Long id, String status, java.math.BigDecimal amount, String createdAt) {}
}
