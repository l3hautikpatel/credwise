// DocumentRepository.java
package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}