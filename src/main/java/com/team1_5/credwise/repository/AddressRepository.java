// AddressRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
}