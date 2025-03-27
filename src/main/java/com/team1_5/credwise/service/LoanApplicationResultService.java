package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.LoanApplicationResultResponse;
import com.team1_5.credwise.exception.ResourceNotFoundException;
import com.team1_5.credwise.model.LoanApplicationResult;
import com.team1_5.credwise.repository.LoanApplicationResultRepository;
import com.team1_5.credwise.util.LoanApplicationResultMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanApplicationResultService {
    private final LoanApplicationResultRepository loanApplicationResultRepository;

    public LoanApplicationResultService(LoanApplicationResultRepository loanApplicationResultRepository) {
        this.loanApplicationResultRepository = loanApplicationResultRepository;
    }

    @Transactional(readOnly = true)
    public LoanApplicationResultResponse getLoanApplicationResult(Long loanApplicationId) {
        LoanApplicationResult result = loanApplicationResultRepository.findByLoanApplicationId(loanApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan Application Result not found for application ID: " + loanApplicationId));

        return new LoanApplicationResultResponse(
                result.getStatus(),
                result.getMessage(),
                result.getEligibilityScore(),
                result.getMaxEligibleAmount(),
                result.getSuggestedInterestRate(),
                result.getSuggestedTerm(),
                result.getEstimatedMonthlyPayment(),
                result.getDecisionFactors().stream()
                        .map(factor -> new LoanApplicationResultResponse.DecisionFactorResponse(factor.getFactor(), factor.getImpact(), factor.getDescription()))
                        .toList()
        );
    }
}
