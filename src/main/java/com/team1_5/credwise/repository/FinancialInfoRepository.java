// FinancialInfoRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.FinancialInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinancialInfoRepository extends JpaRepository<FinancialInfo, Long> {
    Optional<FinancialInfo> findByUserId(Long userId);
    Optional<FinancialInfo> findByLoanApplicationId(Long loanApplicationId);
}