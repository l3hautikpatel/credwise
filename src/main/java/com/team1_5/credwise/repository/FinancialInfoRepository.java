// FinancialInfoRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.FinancialInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialInfoRepository extends JpaRepository<FinancialInfo, Long> {
    Optional<FinancialInfo> findByUserId(Long userId);
    Optional<FinancialInfo> findByLoanApplicationId(Long loanApplicationId);
    
    /**
     * Find the latest financial info for a user based on last_updated timestamp
     */
    @Query("SELECT f FROM FinancialInfo f WHERE f.user.id = :userId ORDER BY f.lastUpdated DESC")
    List<FinancialInfo> findLatestByUserId(Long userId);
}