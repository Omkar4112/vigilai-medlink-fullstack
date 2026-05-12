package com.vigilai.dto;

import lombok.Data;

public class VitalDTOs {

    @Data
    public static class VitalRequest {
        public Long   patientId;
        public String clinicId;
        public String phoneNumber;   // auto-create patient if no patientId
        public String fullName;
        public int    age;
        public String gender;
        public String medicalHistory;
        public String emergencyType;
        public String clinicalNotes;

        public int    heart_rate;
        public double spo2;
        public int    respiratory_rate;
        public int    systolic_bp;
        public int    diastolic_bp;
        public double temperature;
    }

    @Data
    public static class AIPredictionRequest {
        public int    age;
        public double heart_rate;
        public double spo2;
        public double respiratory_rate;
        public double systolic_bp;
        public double diastolic_bp;
        public double temperature;
        public String age_group;      // NEONATAL / PEDIATRIC / ADULT
        public String emergency_type;
    }

    @Data
    public static class AIPredictionResponse {
        public double  risk_score;
        public String  risk_level;
        public String  source;
        public double  confidence;
        public String[] top_features;
        public String  explanation;
        public String  treatment_recs;
        public String  paramedic_guidance;
    }

    @Data
    public static class LLMRequest {
        public VitalRequest vitals;
        public AIPredictionResponse prediction;
        public String patientHistory;
    }

    @Data
    public static class LLMResponse {
        public String explanation;
        public String treatmentRecs;
        public String paramedicGuidance;
    }
}
