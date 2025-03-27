package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.FinancialSummaryResponse;
import com.team1_5.credwise.model.FinancialSummary;
import com.team1_5.credwise.repository.FinancialSummaryRepository;
import com.team1_5.credwise.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

@Service
public class FinancialSummaryService {

    private final FinancialSummaryRepository repository;

    public FinancialSummaryService(FinancialSummaryRepository repository) {
        this.repository = repository;
    }

    public FinancialSummaryResponse getFinancialSummary(Long userId) {
        FinancialSummary summary = repository.findLatestByUserId(userId);

        if (summary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Financial summary not found.");
        }

        return new FinancialSummaryResponse(
                summary.getMonthlyIncome(),
                summary.getMonthlyExpenses(),
                summary.getCreditScore(),
                summary.getScoreRange(),
                summary.getLastUpdated()
        );
    }
}
