package com.team1_5.credwise.util;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.model.DecisionFactor;
import com.team1_5.credwise.model.LoanApplicationResult;
import java.util.List;
import java.util.stream.Collectors;

public class LoanApplicationResultMapper {
    public static LoanApplicationResultResponse toResponse(LoanApplicationResult result) {
        LoanApplicationResultResponse response = new LoanApplicationResultResponse();
        response.setStatus(result.getStatus());
        response.setMessage(result.getMessage());
        response.setEligibilityScore(result.getEligibilityScore());
        response.setMaxEligibleAmount(result.getMaxEligibleAmount());
        response.setSuggestedInterestRate(result.getSuggestedInterestRate());
        response.setSuggestedTerm(result.getSuggestedTerm());
        response.setEstimatedMonthlyPayment(result.getEstimatedMonthlyPayment());

        // Convert decision factors
        List<LoanApplicationResultResponse.DecisionFactorResponse> decisionFactors =
                result.getDecisionFactors().stream().map(df -> {
                    LoanApplicationResultResponse.DecisionFactorResponse factorResponse = new LoanApplicationResultResponse.DecisionFactorResponse();
                    factorResponse.setFactor(df.getFactor());
                    factorResponse.setImpact(df.getImpact());
                    factorResponse.setDescription(df.getDescription());
                    return factorResponse;
                }).collect(Collectors.toList());

        response.setDecisionFactors(decisionFactors);
        return response;
    }
}
