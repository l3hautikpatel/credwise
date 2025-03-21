package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :retentionDate")
    int deleteByTimestampBefore(LocalDateTime retentionDate);
}