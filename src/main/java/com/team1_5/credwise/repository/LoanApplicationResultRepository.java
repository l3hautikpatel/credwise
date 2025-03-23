package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.LoanApplicationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationResultRepository extends JpaRepository<LoanApplicationResult, Long> {
}