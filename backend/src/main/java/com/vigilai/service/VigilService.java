package com.vigilai.service;

import com.vigilai.model.*;
import com.vigilai.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class VigilService {

    @Autowired private AlertRepository alertRepo;
    @Autowired private HospitalRepository hospitalRepo;
    @Autowired private AuditLogService auditLog;

    // ─────────────────────────────────────────────
    // ALERT METHODS
    // ─────────────────────────────────────────────

    public Alert getAlert(Long alertId) {
        return alertRepo.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
    }

    public Alert updateAlert(Alert alert) {
        return alertRepo.save(alert);
    }

    public List<Alert> getPendingAlerts() {
        return alertRepo.findByClinicianDecisionOrderByAlertTimestampDesc("PENDING");
    }

    public void logAlert(Alert alert, String action) {
        String clinician = Optional.ofNullable(alert.getClinicianId()).orElse("SYSTEM");

        auditLog.logAction(
                action,
                "ALERT",
                String.valueOf(alert.getAlertId()),
                clinician,
                null,
                alert.getClinicianDecision()
        );
    }

    // ─────────────────────────────────────────────
    // DISPATCH LOGIC
    // ─────────────────────────────────────────────

    public void dispatchToAmbulance(Alert alert) {

        Hospital hospital = selectBestHospital(alert);
        int eta = calculateETA(hospital.getDistanceKm());

        alert.setDispatchStatus("DISPATCHED");
        alert.setHospitalId(hospital.getHospitalId());
        alert.setDispatchedAt(LocalDateTime.now());

        updateAlert(alert);

        auditLog.logAction(
                "DISPATCH_SENT",
                "DISPATCH",
                String.valueOf(alert.getAlertId()),
                "MEDLINK_SYSTEM",
                null,
                "Hospital=" + hospital.getName() + ", ETA=" + eta + "min"
        );

        log.info("Dispatch: alert={}, hospital={}, ETA={}min",
                alert.getAlertId(), hospital.getName(), eta);
    }

    // ─────────────────────────────────────────────
    // HOSPITAL SELECTION
    // ─────────────────────────────────────────────

    public Hospital selectBestHospital(Alert alert) {

        List<Hospital> hospitals = hospitalRepo.findByIsActiveTrue();
        if (hospitals.isEmpty()) {
            throw new RuntimeException("No active hospitals available");
        }

        double lat = alert.getClinicLatitude() != null ? alert.getClinicLatitude() : 12.9716;
        double lng = alert.getClinicLongitude() != null ? alert.getClinicLongitude() : 77.5946;

        return hospitals.stream()
                .map(h -> {
                    double distance = haversine(
                            lat,
                            lng,
                            safeDouble(h.getLatitude()),
                            safeDouble(h.getLongitude())
                    );

                    double score = score(h, alert.getEmergencyType(), distance);

                    h.setDistanceKm(distance);

                    return Map.entry(h, score);
                })
                .sorted(Map.Entry.<Hospital, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    // ─────────────────────────────────────────────
    // SCORING LOGIC (FIXED + SAFE)
    // ─────────────────────────────────────────────

    private double score(Hospital h, String emergencyType, double distance) {

        double mortality = safeDouble(h.getSepsisMortalityRate(), 50);

        int availableBeds = (h.getTotalIcuBeds() != null && h.getOccupiedBeds() != null)
                ? (h.getTotalIcuBeds() - h.getOccupiedBeds())
                : 0;

        double score = 0;

        // Lower mortality is better
        score += (100 - mortality) * 0.35;

        // ICU beds availability
        score += Math.min(availableBeds * 10.0, 100) * 0.30;

        // Distance (closer is better)
        score += Math.max(0, 100 - distance) * 0.20;

        // Trauma center priority
        score += (Boolean.TRUE.equals(h.getIsLevel1Trauma()) ? 100 : 0) * 0.10;

        // Specialization match
        if (emergencyType != null &&
                h.getSpecializations() != null &&
                h.getSpecializations().contains(emergencyType)) {
            score += 5;
        }

        return score;
    }

    // ─────────────────────────────────────────────
    // ETA CALCULATION
    // ─────────────────────────────────────────────

    private int calculateETA(Double distanceKm) {
        if (distanceKm == null) return 30;

        return (int) Math.ceil((distanceKm / 40.0) * 60) + 5;
    }

    // ─────────────────────────────────────────────
    // HAVERSINE FORMULA
    // ─────────────────────────────────────────────

    private double haversine(double lat1, double lon1, double lat2, double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─────────────────────────────────────────────
    // NULL SAFE HELPERS
    // ─────────────────────────────────────────────

    private double safeDouble(Number value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private double safeDouble(Number value, double defaultVal) {
        return value != null ? value.doubleValue() : defaultVal;
    }
}