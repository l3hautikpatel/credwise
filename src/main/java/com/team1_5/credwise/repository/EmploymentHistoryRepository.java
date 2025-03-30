package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.EmploymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmploymentHistoryRepository extends JpaRepository<EmploymentHistory, Long> {
    List<EmploymentHistory> findByFinancialInfoId(Long financialInfoId);
}