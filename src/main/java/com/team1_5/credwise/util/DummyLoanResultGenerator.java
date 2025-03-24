// src/main/java/com/team1_5/credwise/util/DummyLoanResultGenerator.java
package com.team1_5.credwise.util;

import com.team1_5.credwise.dto.LoanResultDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DummyLoanResultGenerator {

    public LoanResultDTO generateDummyResult() {
        return new LoanResultDTO(
                85,                         // eligibilityScore (0-100)
                new BigDecimal("25000.00"),  // maxEligibleAmount
                "4.25% APR",                // suggestedInterestRate
                60,                         // suggestedTerm (months)
                new BigDecimal("462.75"),    // estimatedMonthlyPayment
                "Excellent (750-850)"       // scoreRange
        );
    }
}