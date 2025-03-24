// FinancialSummaryRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.FinancialSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialSummaryRepository extends JpaRepository<FinancialSummary, Long> {
}