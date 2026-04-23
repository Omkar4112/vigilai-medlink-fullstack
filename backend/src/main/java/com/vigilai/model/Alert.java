package com.vigilai.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;  // BUG FIX: was set via UUID.randomUUID() — now auto-generated Long

    // BUG FIX: use @ManyToOne relationship, NOT a raw patientId field
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Patient patient;

    @Column(name = "clinic_id", nullable = false)
    private String clinicId;

    // BUG FIX: all numeric scores use Double consistently (not mixed BigDecimal/Double)
    @Column(name = "risk_score")
    private Double riskScore;

    private String severity;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "emergency_type")
    private String emergencyType;

    @Builder.Default
    private String status = "NEW";

    @Column(name = "clinician_decision")
    private String clinicianDecision;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // AI Explainability
    @Column(name = "llm_explanation", columnDefinition = "TEXT")
    private String llmExplanation;

    @Column(name = "treatment_recs", columnDefinition = "TEXT")
    private String treatmentRecs;

    @Column(name = "paramedic_guidance", columnDefinition = "TEXT")
    private String paramedicGuidance;

    @Column(name = "alert_timestamp")
    private LocalDateTime alertTimestamp;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // BUG FIX: confidence is Double, not BigDecimal
    private Double confidence;
    private String modelVersion;

    @ElementCollection
    @CollectionTable(name = "alert_features", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "feature")
    private List<String> topFeatures;

    // Clinician actions
    private String clinicianId;
    private LocalDateTime decisionAt;
    private String holdReason;

    // Dispatch / Hospital
    private String dispatchStatus;
    private Long hospitalId;
    private LocalDateTime dispatchedAt;

    // Clinic location
    private Double clinicLatitude;
    private Double clinicLongitude;

    // Vital snapshot
    private Integer heartRate;
    private BigDecimal temperature;
    private Integer respiratoryRate;
    private Integer bpSystolic;
    private Integer bpDiastolic;
    private Integer spo2;
    private Integer patientAge;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (alertTimestamp == null) alertTimestamp = LocalDateTime.now();
        if (status == null) status = "NEW";
        if (clinicianDecision == null) clinicianDecision = "PENDING";
        if (dispatchStatus == null) dispatchStatus = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
