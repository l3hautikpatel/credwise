// PersonalInfoRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.PersonalInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalInfoRepository extends JpaRepository<PersonalInfo, Long> {
}