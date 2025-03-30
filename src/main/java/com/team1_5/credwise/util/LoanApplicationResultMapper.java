package com.team1_5.credwise.util;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.model.DecisionFactor;
import com.team1_5.credwise.model.LoanApplicationResult;
import java.util.List;
import java.util.stream.Collectors;

public class LoanApplicationResultMapper {
    public static LoanApplicationResultResponse toResponse(LoanApplicationResult result) {
        // Convert decision factors
        List<LoanApplicationResultResponse.DecisionFactorResponse> decisionFactors =
                result.getDecisionFactors().stream().map(df -> 
                    new LoanApplicationResultResponse.DecisionFactorResponse(
                        df.getFactor(),
                        df.getImpact(),
                        df.getDescription()
                    )
                ).collect(Collectors.toList());

        return new LoanApplicationResultResponse(
            result.getStatus(),
            result.getMessage(),
            result.getEligibilityScore(),
            result.getMaxEligibleAmount(),
            result.getSuggestedInterestRate(),
            result.getSuggestedTerm(),
            result.getEstimatedMonthlyPayment(),
            decisionFactors
        );
    }
}
