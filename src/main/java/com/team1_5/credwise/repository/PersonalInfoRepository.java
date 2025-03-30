// PersonalInfoRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.PersonalInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonalInfoRepository extends JpaRepository<PersonalInfo, Long> {
    Optional<PersonalInfo> findByLoanApplicationId(Long loanApplicationId);
}