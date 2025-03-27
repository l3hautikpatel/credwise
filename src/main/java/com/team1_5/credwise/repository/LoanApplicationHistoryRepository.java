package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanApplicationHistoryRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByUser_Id(Long userId);
}
