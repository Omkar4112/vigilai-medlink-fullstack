package com.vigilai.service;

import com.vigilai.model.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class WebSocketService {

    @Autowired private SimpMessagingTemplate ws;

    /** Push new alert to all hospital dashboards */
    public void pushAlert(Alert alert) {
        try {
            ws.convertAndSend("/topic/alerts", Map.of(
                "type",      "NEW_ALERT",
                "alertId",   alert.getAlertId(),
                "riskLevel", alert.getRiskLevel(),
                "riskScore", alert.getRiskScore(),
                "severity",  alert.getSeverity(),
                "clinicId",  alert.getClinicId(),
                "patientAge", alert.getPatientAge(),
                "timestamp", alert.getAlertTimestamp().toString()
            ));
        } catch (Exception e) {
            log.warn("WS push failed: {}", e.getMessage());
        }
    }

    /** Push alert status update (approved/held/dispatched) */
    public void pushAlertUpdate(Alert alert) {
        try {
            ws.convertAndSend("/topic/alerts/updates", Map.of(
                "type",              "ALERT_UPDATE",
                "alertId",           alert.getAlertId(),
                "clinicianDecision", alert.getClinicianDecision(),
                "dispatchStatus",    alert.getDispatchStatus() != null ? alert.getDispatchStatus() : "",
                "hospitalId",        alert.getHospitalId() != null ? alert.getHospitalId() : 0
            ));
        } catch (Exception e) {
            log.warn("WS update push failed: {}", e.getMessage());
        }
    }

    /** Push ICU bed count changes */
    public void pushBedUpdate(Long hospitalId, int available, int total) {
        try {
            ws.convertAndSend("/topic/hospital/" + hospitalId + "/beds", Map.of(
                "type",      "BED_UPDATE",
                "available", available,
                "total",     total,
                "occupied",  total - available
            ));
        } catch (Exception e) {
            log.warn("WS bed update failed: {}", e.getMessage());
        }
    }
}
