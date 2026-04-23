package com.vigilai.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vitals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vital_id")
    private Long vitalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "clinic_id", nullable = false)
    private String clinicId;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "blood_pressure_systolic")
    private Integer bloodPressureSystolic;

    @Column(name = "blood_pressure_diastolic")
    private Integer bloodPressureDiastolic;

    private Integer spo2;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "emergency_type")
    private String emergencyType;

    @Column(name = "vital_timestamp", nullable = false)
    private LocalDateTime vitalTimestamp;

    @Column(name = "sync_status", length = 20)
    @Builder.Default
    private String syncStatus = "PENDING";

    @Column(name = "is_encrypted")
    @Builder.Default
    private Boolean isEncrypted = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (vitalTimestamp == null) vitalTimestamp = LocalDateTime.now();
    }
}
