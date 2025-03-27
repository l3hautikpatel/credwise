// FinancialInfoRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.FinancialInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialInfoRepository extends JpaRepository<FinancialInfo, Long> {
}