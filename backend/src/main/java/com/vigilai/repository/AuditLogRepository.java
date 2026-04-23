package com.vigilai.repository;

import com.vigilai.model.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    @Query("SELECT a FROM AuditLogEntry a ORDER BY a.logId DESC")
    Page<AuditLogEntry> findAllPaged(Pageable pageable);

    @Query(value = "SELECT * FROM audit_log_worm ORDER BY log_id DESC LIMIT 1", nativeQuery = true)
    Optional<AuditLogEntry> findLatest();

    @Query("SELECT a FROM AuditLogEntry a ORDER BY a.logId ASC")
    List<AuditLogEntry> findAllOrdered();
}
