package com.team1_5.credwise.service;


import com.team1_5.credwise.dto.CreditScoreDetails;
import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CreditScoreService {
    public boolean checkEligibility(int creditScore) {
        // Basic credit score eligibility check
        return creditScore >= 600; // Minimum credit score threshold
    }

    private final CreditBureauIntegration creditBureauIntegration;

    public CreditScoreService(CreditBureauIntegration creditBureauIntegration) {
        this.creditBureauIntegration = creditBureauIntegration;
    }

    public CreditScoreDetails fetchCreditScore(User user) {
        // Integrate with credit bureau to fetch credit score
        return creditBureauIntegration.fetchCreditScore(user);
    }

    public boolean isEligibleForLoan(CreditScoreDetails creditScore, LoanApplication application) {
        // Implement complex eligibility criteria
        return creditScore.getScore() >= 650 &&
                calculateDebtToIncomeRatio(application) <= 0.36;
    }

    private BigDecimal calculateDebtToIncomeRatio(LoanApplication application) {
        BigDecimal monthlyIncome = application.getFinancialInfo().getMonthlyIncome();
        BigDecimal totalMonthlyDebt = calculateTotalMonthlyDebt(application);

        return totalMonthlyDebt.divide(monthlyIncome, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalMonthlyDebt(LoanApplication application) {
        return application.getFinancialInfo().getDebts().stream()
                .map(Debt::getMonthlyPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public CreditScoreDetails fetchCreditScore(User user) {
        // In a real-world scenario, this would integrate with credit bureaus
        // For now, it's a placeholder
        return new CreditScoreDetails(
//                user.getCreditScore(),
                // Other credit-related details
        );
    }
}