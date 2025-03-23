// DebtRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtRepository extends JpaRepository<Debt, Long> {
}