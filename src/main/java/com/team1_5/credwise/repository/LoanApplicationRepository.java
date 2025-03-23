// LoanApplicationRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
}