package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.FinancialSummaryResponse;
import com.team1_5.credwise.model.FinancialInfo;
import com.team1_5.credwise.repository.FinancialInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;
import java.util.List;

@Service
public class FinancialSummaryService {

    private final FinancialInfoRepository financialInfoRepository;

    public FinancialSummaryService(FinancialInfoRepository financialInfoRepository) {
        this.financialInfoRepository = financialInfoRepository;
    }

    /**
     * Get the latest financial summary for a user
     */
    public FinancialSummaryResponse getFinancialSummary(Long userId) {
        // Get the latest financial info for the user
        List<FinancialInfo> financialInfoList = financialInfoRepository.findLatestByUserId(userId);
        
        if (financialInfoList == null || financialInfoList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Financial information not found for this user.");
        }
        
        // Get the latest entry (first in the list since we ordered by lastUpdated DESC)
        FinancialInfo latestInfo = financialInfoList.get(0);
        
        // Create and return the response with both the user-provided and system-generated credit scores
        return new FinancialSummaryResponse(
                latestInfo.getMonthlyIncome(),
                latestInfo.getMonthlyExpenses(),
                latestInfo.getCreditScore(),        // User-provided credit score
                latestInfo.getSystemCreditScore(),  // System-generated credit score
                latestInfo.getLastUpdated()
        );
    }
}
