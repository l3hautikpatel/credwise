package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.DecisionFactor;
import com.team1_5.credwise.model.LoanApplicationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionFactorRepository extends JpaRepository<DecisionFactor, Long> {
    List<DecisionFactor> findByResult(LoanApplicationResult result);
    void deleteByResult(LoanApplicationResult result);
}
