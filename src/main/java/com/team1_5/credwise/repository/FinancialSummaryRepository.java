package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.FinancialSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialSummaryRepository extends JpaRepository<FinancialSummary, Long> {

    @Query("SELECT f FROM FinancialSummary f WHERE f.user.id = :userId ORDER BY f.lastUpdated DESC LIMIT 1")
    FinancialSummary findLatestByUserId(Long userId);
}
