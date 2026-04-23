package com.vigilai.service;

import com.vigilai.dto.VitalDTOs.*;
import com.vigilai.model.Patient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class LLMService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai.llm.url}")
    private String llmUrl;

    // ─────────────────────────────────────────────
    // RESPONSE CLASS (BETTER WITH @Data)
    // ─────────────────────────────────────────────
    @Data
    public static class LLMResult {
        private String explanation;
        private String treatmentRecs;
        private String paramedicGuidance;
    }

    // ─────────────────────────────────────────────
    // MAIN METHOD
    // ─────────────────────────────────────────────
    public LLMResult explain(VitalRequest vitals,
                             AIPredictionResponse prediction,
                             Patient patient) {

        try {
            Map<String, Object> payload = buildPayload(vitals, prediction, patient);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(payload, headers);

            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(
                            llmUrl,
                            HttpMethod.POST,
                            request,
                            new ParameterizedTypeReference<>() {}
                    );

            return parseResponse(response.getBody());

        } catch (Exception e) {
            log.warn("LLM service unavailable — using fallback: {}", e.getMessage());
            return generateFallbackGuidance(vitals, prediction, patient);
        }
    }

    // ─────────────────────────────────────────────
    // BUILD PAYLOAD
    // ─────────────────────────────────────────────
    private Map<String, Object> buildPayload(VitalRequest v,
                                            AIPredictionResponse pred,
                                            Patient patient) {

        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> vitals = new HashMap<>();
        vitals.put("heart_rate", v.getHeart_rate());
        vitals.put("temperature", v.getTemperature());
        vitals.put("respiratory_rate", v.getRespiratory_rate());
        vitals.put("systolic_bp", v.getSystolic_bp());
        vitals.put("diastolic_bp", v.getDiastolic_bp());
        vitals.put("spo2", v.getSpo2());
        vitals.put("age", v.getAge());
        vitals.put("age_group", safeString(patient.getAgeGroup(), "ADULT"));

        payload.put("vitals", vitals);
        payload.put("risk_score", safeDouble(pred.getRisk_score()));
        payload.put("risk_level", safeString(pred.getRisk_level(), "UNKNOWN"));
        payload.put("emergency_type",
                v.getEmergencyType() != null ? v.getEmergencyType() : "SEPSIS");
        payload.put("medical_history",
                safeString(patient.getMedicalHistory(), "Unknown"));

        return payload;
    }

    // ─────────────────────────────────────────────
    // PARSE RESPONSE SAFELY
    // ─────────────────────────────────────────────
    private LLMResult parseResponse(Map<String, Object> body) {

        LLMResult result = new LLMResult();

        if (body != null) {
            result.setExplanation(safeString(body.get("explanation"), ""));
            result.setTreatmentRecs(safeString(body.get("treatment_recs"), ""));
            result.setParamedicGuidance(safeString(body.get("paramedic_guidance"), ""));
        }

        return result;
    }

    // ─────────────────────────────────────────────
    // FALLBACK LOGIC
    // ─────────────────────────────────────────────
    private LLMResult generateFallbackGuidance(
            VitalRequest v,
            AIPredictionResponse pred,
            Patient patient) {

        LLMResult r = new LLMResult();
        String ageGroup = safeString(patient.getAgeGroup(), "ADULT");

        r.setExplanation(String.format(
                "AI analysis of %s patient (age %d) shows %.0f%% %s risk. " +
                "Vitals: HR=%d bpm, Temp=%.1f°C, RR=%d/min, BP=%d/%d mmHg, SpO2=%d%%.",
                ageGroup.toLowerCase(),
                v.getAge(),
                safeDouble(pred.getRisk_score()) * 100,
                safeString(pred.getRisk_level(), "UNKNOWN"),
                v.getHeart_rate(),
                v.getTemperature(),
                v.getRespiratory_rate(),
                v.getSystolic_bp(),
                v.getDiastolic_bp(),
                (int) v.getSpo2()
        ));

        r.setTreatmentRecs(buildTreatmentRecs(v, pred, ageGroup));
        r.setParamedicGuidance(buildParamedicGuidance(v, ageGroup));

        return r;
    }

    // ─────────────────────────────────────────────
    // TREATMENT RECOMMENDATIONS
    // ─────────────────────────────────────────────
    private String buildTreatmentRecs(VitalRequest v,
                                     AIPredictionResponse pred,
                                     String ageGroup) {

        StringBuilder sb = new StringBuilder();
        sb.append("IMMEDIATE ACTIONS:\n");

        if (v.getSpo2() < 92)
            sb.append("• Oxygen support — target SpO2 ≥ 94%\n");

        if (v.getSystolic_bp() < 100)
            sb.append("• IV fluids — Normal Saline 30 mL/kg\n");

        if (v.getTemperature() > 38.5)
            sb.append("• Antipyretics + blood cultures\n");

        if (v.getHeart_rate() > 120)
            sb.append("• Continuous cardiac monitoring\n");

        switch (ageGroup) {
            case "NEONATAL":
                sb.append("• NICU referral\n• Glucose monitoring\n");
                break;
            case "PEDIATRIC":
                sb.append("• Pediatric protocol (PALS)\n");
                break;
            default:
                sb.append("• Sepsis protocol (Sepsis-3)\n");
        }

        if ("CRITICAL".equalsIgnoreCase(pred.getRisk_level())) {
            sb.append("\n⚠️ CRITICAL: Immediate ICU transfer\n");
        }

        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────
    // PARAMEDIC GUIDANCE
    // ─────────────────────────────────────────────
    private String buildParamedicGuidance(VitalRequest v, String ageGroup) {

        return String.format(
                "EN-ROUTE PROTOCOL (%s):\n" +
                        "1. Oxygen support\n" +
                        "2. IV access\n" +
                        "3. Monitor vitals every 5 min\n" +
                        "4. Transport immediately\n" +
                        "Vitals: HR=%d, Temp=%.1f°C, RR=%d, BP=%d/%d, SpO2=%d%%",
                ageGroup,
                v.getHeart_rate(),
                v.getTemperature(),
                v.getRespiratory_rate(),
                v.getSystolic_bp(),
                v.getDiastolic_bp(),
                (int) v.getSpo2()
        );
    }

    // ─────────────────────────────────────────────
    // SAFE HELPERS
    // ─────────────────────────────────────────────
    private String safeString(Object value, String defaultVal) {
        return value != null ? value.toString() : defaultVal;
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}