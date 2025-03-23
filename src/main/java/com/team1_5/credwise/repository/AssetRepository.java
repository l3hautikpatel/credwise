// AssetRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {
}