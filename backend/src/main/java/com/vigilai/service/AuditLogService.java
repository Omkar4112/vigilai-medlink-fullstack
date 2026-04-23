package com.vigilai.service;

import com.vigilai.model.AuditLogEntry;
import com.vigilai.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuditLogService {

    private static final DateTimeFormatter HASH_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired private AuditLogRepository auditRepo;

    public void logAction(String action, String entityType, String entityId,
                          String userId, Object oldValue, Object newValue) {
        try {
            String hashPrev = auditRepo.findLatest()
                    .map(AuditLogEntry::getHashCurrent)
                    .orElse("GENESIS");

            AuditLogEntry entry = AuditLogEntry.builder()
                    .action(action).entityType(entityType).entityId(entityId)
                    .userId(userId)
                    .oldValue(oldValue != null ? oldValue.toString() : null)
                    .newValue(newValue != null ? newValue.toString() : null)
                    .timestamp(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                    .hashPrevious(hashPrev).immutable(true)
                    .build();

            String hash = sha256(entry);
            entry.setHashCurrent(hash);
            entry.setSignature("HMAC_" + hash.substring(0, 16));

            auditRepo.save(entry);

        } catch (Exception e) {
            log.error("AUDIT WRITE FAILED [{}/{}]: {}", entityType, entityId, e.getMessage());
        }
    }

    public boolean verifyIntegrity() {
        List<AuditLogEntry> entries = auditRepo.findAllOrdered();
        if (entries.isEmpty()) return true;
        String prev = "GENESIS";
        for (AuditLogEntry e : entries) {
            if (e.getHashPrevious() != null && !e.getHashPrevious().equals(prev)) {
                log.error("AUDIT BREACH at log_id={}", e.getLogId()); return false;
            }
            try {
                if (!sha256(e).equals(e.getHashCurrent())) {
                    log.error("HASH MISMATCH at log_id={}", e.getLogId()); return false;
                }
            } catch (Exception ex) { return false; }
            prev = e.getHashCurrent();
        }
        return true;
    }

    public Page<AuditLogEntry> getPagedLogs(int page, int size) {
        return auditRepo.findAllPaged(PageRequest.of(page, size));
    }

    public Map<String, Object> getStats() {
        return Map.of("total", auditRepo.count(),
                      "integrity", verifyIntegrity(),
                      "checked", LocalDateTime.now());
    }

    private String sha256(AuditLogEntry e) throws Exception {
        String tsStr = e.getTimestamp() != null ? e.getTimestamp().format(HASH_FORMAT) : "NULL";
        String oldV = e.getOldValue() != null ? e.getOldValue() : "NULL";
        String newV = e.getNewValue() != null ? e.getNewValue() : "NULL";
        
        String data = String.join("|", 
                e.getAction(), 
                e.getEntityType(), 
                e.getEntityId(),
                tsStr, 
                oldV, 
                newV, 
                e.getHashPrevious());
                
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
