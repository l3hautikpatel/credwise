package com.team1_5.credwise.util;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import org.springframework.stereotype.Component;

@Component
public class LoanResultCalculator {
    public boolean calculateResult(LoanApplicationRequest request, boolean isCreditValid) {
        // Temporary implementation
        return isCreditValid;
    }
}