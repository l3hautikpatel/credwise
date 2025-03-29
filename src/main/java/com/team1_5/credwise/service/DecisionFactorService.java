package com.team1_5.credwise.service;

import com.team1_5.credwise.model.DecisionFactor;
import com.team1_5.credwise.model.LoanApplicationResult;
import com.team1_5.credwise.repository.DecisionFactorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DecisionFactorService {

    private final DecisionFactorRepository decisionFactorRepository;

    public DecisionFactorService(DecisionFactorRepository decisionFactorRepository) {
        this.decisionFactorRepository = decisionFactorRepository;
    }

    /**
     * Create decision factors for a loan application result based on credit evaluation
     * @param result The loan application result entity
     * @param creditEvaluationData Credit evaluation data from the scoring system
     */
    @Transactional
    public void createDecisionFactors(LoanApplicationResult result, Map<String, Object> creditEvaluationData) {
        // First delete any existing decision factors for this result
        decisionFactorRepository.deleteByResult(result);

        List<DecisionFactor> factors = new ArrayList<>();

        // Credit Score factor
        int creditScore = ((Number) creditEvaluationData.get("creditScore")).intValue();
        String creditScoreRating = (String) creditEvaluationData.get("creditScoreRating");
        String creditScoreImpact = creditScore >= 600 ? "Positive" : "Negative";
        String creditScoreDescription = creditScore >= 600 
                ? "Credit score of " + creditScore + " (" + creditScoreRating + ") is sufficient." 
                : "Credit score of " + creditScore + " (" + creditScoreRating + ") is below recommended minimum.";
                
        factors.add(createFactor(result, "Credit Score", creditScoreImpact, creditScoreDescription));

        // Debt-to-Income Ratio factor
        double dti = ((Number) creditEvaluationData.get("dti")).doubleValue();
        String dtiImpact = dti < 0.4 ? "Positive" : "Negative";
        String dtiDescription = dti < 0.4 
                ? "Debt-to-income ratio of " + String.format("%.1f%%", dti * 100) + " is within acceptable range." 
                : "Debt-to-income ratio of " + String.format("%.1f%%", dti * 100) + " exceeds recommended maximum.";
                
        factors.add(createFactor(result, "Debt-to-Income Ratio", dtiImpact, dtiDescription));

        // Employment Stability factor
        String employmentStability = (String) creditEvaluationData.get("employmentStability");
        String employmentImpact = employmentStability.equals("Stable") ? "Positive" : "Negative";
        String employmentDescription = employmentStability.equals("Stable") 
                ? "Employment status indicates stability." 
                : "Employment history shows insufficient stability.";
                
        factors.add(createFactor(result, "Employment Stability", employmentImpact, employmentDescription));

        // Payment History factor
        String paymentHistoryRating = (String) creditEvaluationData.get("paymentHistoryRating");
        String paymentHistoryImpact = paymentHistoryRating.equals("Excellent") ? "Positive" : "Negative";
        String paymentHistoryDescription = paymentHistoryRating.equals("Excellent") 
                ? "Payment history shows consistent on-time payments." 
                : "Payment history indicates issues with timely repayments.";
                
        factors.add(createFactor(result, "Payment History", paymentHistoryImpact, paymentHistoryDescription));

        // Credit Score Accuracy (only if user provided a score)
        if (creditEvaluationData.containsKey("isScoreAccurate")) {
            boolean isScoreAccurate = (boolean) creditEvaluationData.get("isScoreAccurate");
            if (!isScoreAccurate) {
                String message = (String) creditEvaluationData.get("creditScoreAccuracyMessage");
                factors.add(createFactor(result, "Credit Score Discrepancy", "Warning", message));
            }
        }

        // Save all factors
        decisionFactorRepository.saveAll(factors);
    }

    private DecisionFactor createFactor(LoanApplicationResult result, String factor, String impact, String description) {
        DecisionFactor decisionFactor = new DecisionFactor();
        decisionFactor.setResult(result);
        decisionFactor.setFactor(factor);
        decisionFactor.setImpact(impact);
        decisionFactor.setDescription(description);
        return decisionFactor;
    }

    /**
     * Get all decision factors for a loan application result
     */
    public List<DecisionFactor> getDecisionFactorsByResult(LoanApplicationResult result) {
        return decisionFactorRepository.findByResult(result);
    }
} 