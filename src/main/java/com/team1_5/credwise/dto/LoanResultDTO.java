// src/main/java/com/team1_5/credwise/dto/LoanResultDTO.java
package com.team1_5.credwise.dto;

import java.math.BigDecimal;

public record LoanResultDTO(
        int eligibilityScore,
        BigDecimal maxEligibleAmount,
        String suggestedInterestRate,
        int suggestedTerm,
        BigDecimal estimatedMonthlyPayment,
        String scoreRange
) {}