package com.team1_5.credwise.service;

import com.team1_5.credwise.model.LoanApplication;
import com.team1_5.credwise.repository.LoanApplicationHistoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LoanApplicationHistoryService {

    private final LoanApplicationHistoryRepository loanApplicationHistoryRepository;

    public LoanApplicationHistoryService(LoanApplicationHistoryRepository loanApplicationHistoryRepository) {
        this.loanApplicationHistoryRepository = loanApplicationHistoryRepository;
    }

    public List<LoanApplication> getLoanApplicationHistoryByUserId(Long userId) {
        return loanApplicationHistoryRepository.findByUser_Id(userId);
    }
}
