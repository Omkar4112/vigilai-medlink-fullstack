package com.vigilai.controller;

import com.vigilai.dto.VitalDTOs.*;
import com.vigilai.model.*;
import com.vigilai.repository.*;
import com.vigilai.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/clinic/vitals")
@RequiredArgsConstructor
@Slf4j
public class VitalController {

    private final VitalRepository vitalRepo;
    private final AlertRepository alertRepo;
    private final PatientRepository patientRepo;
    private final PatientService patientService;
    private final AIService aiService;
    private final LLMService llmService;
    private final AuditLogService auditLog;
    private final WebSocketService wsService;
    private final TriageFlagRepository triageRepo;

    @PostMapping
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    public ResponseEntity<Map<String, Object>> submitVitals(@RequestBody VitalRequest req) {

        // 1. Resolve patient
        Patient patient;
        if (req.getPatientId() != null) {
            patient = patientRepo.findById(req.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found: " + req.getPatientId()));
        } else {
            patient = patientService.findOrCreate(
                    req.getClinicId(),
                    req.getPhoneNumber(),
                    req.getAge(),
                    req.getGender(),
                    req.getFullName()
            );
        }

        // 2. Save vital
        Vital vital = Vital.builder()
                .patient(patient)
                .clinicId(req.getClinicId() != null ? req.getClinicId() : patient.getClinicId())
                .heartRate(req.getHeart_rate())
                .temperature(BigDecimal.valueOf(req.getTemperature()))
                .respiratoryRate(req.getRespiratory_rate())
                .bloodPressureSystolic(req.getSystolic_bp())
                .bloodPressureDiastolic(req.getDiastolic_bp())
                .spo2((int) req.getSpo2())
                .clinicalNotes(req.getClinicalNotes())
                .emergencyType(req.getEmergencyType())
                .vitalTimestamp(LocalDateTime.now())
                .syncStatus("SYNCED")
                .build();

        vitalRepo.save(vital);

        // 3. Triage
        TriageFlag triage = runTriage(vital, patient);

        // 4. AI Prediction
        AIPredictionResponse aiResp = aiService.getPrediction(buildAIRequest(req, patient));

        boolean isHighRisk = "HIGH".equalsIgnoreCase(aiResp.getRisk_level())
                || "CRITICAL".equalsIgnoreCase(aiResp.getRisk_level());

        String explanation = null;
        String treatmentRecs = null;
        String paramedicGuidance = null;

        // 5. LLM (only if high risk)
        if (isHighRisk) {
            try {
                LLMService.LLMResult llm = llmService.explain(req, aiResp, patient);
                explanation = llm.getExplanation();
                treatmentRecs = llm.getTreatmentRecs();
                paramedicGuidance = llm.getParamedicGuidance();
            } catch (Exception e) {
                log.warn("LLM failed: {}", e.getMessage());
            }
        }

        // 6. Create Alert
        if (isHighRisk) {
            Alert alert = Alert.builder()
                    .patient(patient)
                    .clinicId(vital.getClinicId())
                    .riskScore(aiResp.getRisk_score())
                    .severity(aiResp.getRisk_level())
                    .riskLevel(aiResp.getRisk_level())
                    .emergencyType(req.getEmergencyType())
                    .confidence(aiResp.getConfidence())
                    .modelVersion("VigilAI_v2.0")
                    .heartRate(req.getHeart_rate())
                    .temperature(BigDecimal.valueOf(req.getTemperature()))
                    .respiratoryRate(req.getRespiratory_rate())
                    .bpSystolic(req.getSystolic_bp())
                    .bpDiastolic(req.getDiastolic_bp())
                    .spo2((int) req.getSpo2())
                    .patientAge(req.getAge())
                    .llmExplanation(explanation)
                    .treatmentRecs(treatmentRecs)
                    .paramedicGuidance(paramedicGuidance)
                    .status("NEW")
                    .clinicianDecision("PENDING")
                    .dispatchStatus("PENDING")
                    .alertTimestamp(LocalDateTime.now())
                    .build();

            Alert saved = alertRepo.save(alert);

            wsService.pushAlert(saved);

            auditLog.logAction(
                    "ALERT_CREATED",
                    "ALERT",
                    String.valueOf(saved.getAlertId()),
                    "AI_ENGINE",
                    null,
                    "risk=" + aiResp.getRisk_score()
            );

            log.warn("🚨 ALERT #{} — {}",
                    saved.getAlertId(),
                    aiResp.getRisk_level());
        }

        // 7. Response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("vitalId", vital.getVitalId());
        response.put("patientId", patient.getPatientId());
        response.put("ageGroup", patient.getAgeGroup());
        response.put("riskLevel", aiResp.getRisk_level());
        response.put("riskScore", aiResp.getRisk_score());
        response.put("triageSeverity", triage.getRuleSeverity());
        response.put("flagCount", triage.getFlagCount());
        response.put("alertCreated", isHighRisk);

        if (isHighRisk) {
            response.put("explanation", explanation);
            response.put("treatmentRecs", treatmentRecs);
            response.put("paramedicGuidance", paramedicGuidance);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    public ResponseEntity<?> getPatientVitals(@PathVariable Long patientId) {
        return ResponseEntity.ok(vitalRepo.findPatientHistory(patientId));
    }

    // ─────────────────────────────────────────────
    // TRIAGE (UNCHANGED BUT SAFE)
    // ─────────────────────────────────────────────
    private TriageFlag runTriage(Vital vital, Patient patient) {

        boolean hrFlag   = vital.getHeartRate() != null && vital.getHeartRate() > 100;
        boolean tempFlag = vital.getTemperature() != null && vital.getTemperature().doubleValue() > 38.5;
        boolean rrFlag   = vital.getRespiratoryRate() != null && vital.getRespiratoryRate() > 24;
        boolean bpFlag   = vital.getBloodPressureSystolic() != null && vital.getBloodPressureSystolic() < 100;
        boolean spo2Flag = vital.getSpo2() != null && vital.getSpo2() < 92;

        String ageGroup = patient.getAgeGroup();

        if ("NEONATAL".equals(ageGroup)) {
            hrFlag = vital.getHeartRate() != null &&
                    (vital.getHeartRate() > 180 || vital.getHeartRate() < 100);
            rrFlag = vital.getRespiratoryRate() != null &&
                    vital.getRespiratoryRate() > 60;
        } else if ("PEDIATRIC".equals(ageGroup)) {
            hrFlag = vital.getHeartRate() != null &&
                    vital.getHeartRate() > 140;
        }

        int flagCount = (hrFlag?1:0)+(tempFlag?1:0)+(rrFlag?1:0)+(bpFlag?1:0)+(spo2Flag?1:0);
        String severity = flagCount >= 2 ? "PRIORITY" : "NORMAL";

        return triageRepo.save(
                TriageFlag.builder()
                        .patient(patient)
                        .vital(vital)
                        .ruleSeverity(severity)
                        .hrFlag(hrFlag)
                        .tempFlag(tempFlag)
                        .rrFlag(rrFlag)
                        .bpFlag(bpFlag)
                        .spo2Flag(spo2Flag)
                        .flagCount(flagCount)
                        .build()
        );
    }

    private AIPredictionRequest buildAIRequest(VitalRequest req, Patient patient) {
        AIPredictionRequest aiReq = new AIPredictionRequest();
        aiReq.setAge(req.getAge());
        aiReq.setHeart_rate(req.getHeart_rate());
        aiReq.setSpo2(req.getSpo2());
        aiReq.setRespiratory_rate(req.getRespiratory_rate());
        aiReq.setSystolic_bp(req.getSystolic_bp());
        aiReq.setDiastolic_bp(req.getDiastolic_bp());
        aiReq.setTemperature(req.getTemperature());
        aiReq.setAge_group(patient.getAgeGroup());
        aiReq.setEmergency_type(req.getEmergencyType());
        return aiReq;
    }
}