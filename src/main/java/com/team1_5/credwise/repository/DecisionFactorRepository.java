package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.DecisionFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DecisionFactorRepository extends JpaRepository<DecisionFactor, Long> {
    List<DecisionFactor> findByResultId(Long resultId);
}
