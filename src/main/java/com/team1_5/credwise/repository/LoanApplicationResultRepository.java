package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.LoanApplicationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanApplicationResultRepository extends JpaRepository<LoanApplicationResult, Long> {
    Optional<LoanApplicationResult> findByLoanApplicationId(Long loanApplicationId);
}