package com.vigilai.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"clinic_id", "phone_number"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "clinic_id", nullable = false)
    private String clinicId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false)
    private Integer age;

    @Column(length = 1)
    private String gender;

    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Derived field - not stored, computed from age */
    @Transient
    public String getAgeGroup() {
        if (age == null) return "ADULT";
        if (age == 0)  return "NEONATAL";
        if (age <= 18) return "PEDIATRIC";
        return "ADULT";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
