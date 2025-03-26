package com.team1_5.credwise.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// dto/FinancialSummaryResponse.java
public record FinancialSummaryResponse(
        @JsonProperty("monthlyIncome") BigDecimal monthlyIncome,
        @JsonProperty("monthlyExpenses") BigDecimal monthlyExpenses,
        @JsonProperty("creditScore") CreditScoreResponse creditScore
) {
    public record CreditScoreResponse(
            @JsonProperty("score") Integer score,
            @JsonProperty("range") String range,
            @JsonProperty("lastUpdated") String lastUpdated
    ) {}
}