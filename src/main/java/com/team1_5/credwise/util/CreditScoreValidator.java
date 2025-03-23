package com.team1_5.credwise.util;

import com.team1_5.credwise.dto.LoanApplicationRequest;
import org.springframework.stereotype.Component;

@Component
public class CreditScoreValidator {
    public boolean validate(LoanApplicationRequest request) {
        // Temporary implementation
        return true;
    }
}