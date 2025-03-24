package com.team1_5.credwise.util;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import com.team1_5.credwise.dto.LoanResultDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LoanResultGenerator {

    public LoanResultDTO generateDummyResult(LoanApplicationRequest request) {
        return new LoanResultDTO(
                75,                          // eligibilityScore
                new BigDecimal("25000.00"),  // maxEligibleAmount
                "4.25% APR",                 // suggestedInterestRate
                60,                          // suggestedTerm
                new BigDecimal("462.75"),     // estimatedMonthlyPayment
                "Good (700-850)"              // scoreRange
        );
    }
}