package com.vigilai.service;

import com.vigilai.dto.VitalDTOs.AIPredictionRequest;
import com.vigilai.dto.VitalDTOs.AIPredictionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class AIService {

    @Autowired private RestTemplate restTemplate;
    @Value("${ai.service.url}") private String aiUrl;

    public AIPredictionResponse getPrediction(AIPredictionRequest req) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<AIPredictionResponse> resp =
                    restTemplate.postForEntity(aiUrl, new HttpEntity<>(req, headers), AIPredictionResponse.class);
            AIPredictionResponse body = resp.getBody();
            if (body == null) throw new RuntimeException("Null response from AI");
            return body;
        } catch (Exception e) {
            log.warn("AI unreachable — fallback: {}", e.getMessage());
            return fallback(req);
        }
    }

    private AIPredictionResponse fallback(AIPredictionRequest req) {
        int f = 0;
        if (req.getHeart_rate()       > 100) f++;
        if (req.getTemperature()      > 38.5) f++;
        if (req.getRespiratory_rate() > 24)   f++;
        if (req.getSystolic_bp()      < 100)  f++;
        if (req.getSpo2()             < 92)   f++;
        double score = Math.min(f * 0.18, 1.0);
        String level = score >= 0.8 ? "CRITICAL" : score >= 0.6 ? "HIGH" : score >= 0.3 ? "MEDIUM" : "LOW";
        AIPredictionResponse r = new AIPredictionResponse();
        r.setRisk_score(score); r.setRisk_level(level);
        r.setSource("RULE_ENGINE_FALLBACK"); r.setConfidence(0.75);
        return r;
    }
}
