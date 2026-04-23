package com.vigilai.controller;

import com.vigilai.model.Alert;
import com.vigilai.repository.AlertRepository;
import com.vigilai.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepo;
    private final AuditLogService auditLog;
    private final WebSocketService wsService;

    @GetMapping("/api/clinic/alerts")
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> clinicAlerts(@RequestParam String clinicId) {
        return ResponseEntity.ok(alertRepo.findByClinicIdOrderByAlertTimestampDesc(clinicId));
    }

    @GetMapping("/api/hospital/alerts")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> hospitalAlerts() {
        return ResponseEntity.ok(alertRepo.findByClinicianDecisionOrderByAlertTimestampDesc("PENDING"));
    }

    @GetMapping("/api/hospital/alerts/dispatched")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> dispatchedAlerts(@RequestParam Long hospitalId) {
        return ResponseEntity.ok(alertRepo.findByHospitalIdOrderByAlertTimestampDesc(hospitalId));
    }

    @PostMapping("/api/hospital/alerts/{id}/approve")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestHeader(value = "X-Clinician-Id", defaultValue = "SYSTEM") String clinicianId,
            org.springframework.security.core.Authentication authentication) {

        try {
            Alert alert = alertRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Alert not found: " + id));

            alert.setClinicianDecision("APPROVED");
            alert.setClinicianId(clinicianId);
            alert.setDecisionAt(LocalDateTime.now());
            alert.setStatus("APPROVED");
            
            // Set dispatch info so it appears on the dispatched page
            alert.setDispatchStatus("DISPATCHED");
            alert.setDispatchedAt(LocalDateTime.now());
            
            if (authentication != null && authentication.getDetails() instanceof String) {
                try {
                    alert.setHospitalId(Long.parseLong((String) authentication.getDetails()));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse hospitalId from token details: {}", authentication.getDetails());
                }
            }

            alertRepo.save(alert);

            auditLog.logAction("ALERT_APPROVED", "ALERT",
                    String.valueOf(id), clinicianId, "PENDING", "APPROVED");

            wsService.pushAlertUpdate(alert);

            return ResponseEntity.ok(Map.of("alertId", id, "status", "APPROVED"));

        } catch (Exception e) {
            log.error("Approve failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/hospital/alerts/{id}/hold")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> hold(
            @PathVariable Long id,
            @RequestHeader(value = "X-Clinician-Id", defaultValue = "SYSTEM") String clinicianId,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            Alert alert = alertRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Alert not found: " + id));

            String reason = (body != null) ? body.getOrDefault("reason", "No reason") : "No reason";

            alert.setClinicianDecision("HOLD");
            alert.setClinicianId(clinicianId);
            alert.setHoldReason(reason);
            alert.setDecisionAt(LocalDateTime.now());
            alert.setStatus("ON_HOLD");

            alertRepo.save(alert);

            auditLog.logAction("ALERT_HELD", "ALERT",
                    String.valueOf(id), clinicianId, "PENDING", "HOLD");

            wsService.pushAlertUpdate(alert);

            return ResponseEntity.ok(Map.of("alertId", id, "status", "ON_HOLD"));

        } catch (Exception e) {
            log.error("Hold failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/hospital/alerts/{id}/dismiss")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> dismiss(
            @PathVariable Long id,
            @RequestHeader(value = "X-Clinician-Id", defaultValue = "SYSTEM") String clinicianId) {

        try {
            Alert alert = alertRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Alert not found: " + id));

            alert.setClinicianDecision("DISMISSED");
            alert.setClinicianId(clinicianId);
            alert.setDecisionAt(LocalDateTime.now());
            alert.setStatus("DISMISSED");

            alertRepo.save(alert);

            auditLog.logAction("ALERT_DISMISSED", "ALERT",
                    String.valueOf(id), clinicianId, "PENDING", "DISMISSED");

            return ResponseEntity.ok(Map.of("alertId", id, "status", "DISMISSED"));

        } catch (Exception e) {
            log.error("Dismiss failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/v1/alerts/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAlert(@PathVariable Long id) {
        return alertRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}