package com.vigilai.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospitals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hospital_id")
    private Long hospitalId;  // BUG FIX: was Integer in old HospitalRepository

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "total_icu_beds")
    @Builder.Default
    private Integer totalIcuBeds = 0;

    @Column(name = "occupied_beds")
    @Builder.Default
    private Integer occupiedBeds = 0;

    @ElementCollection
    @CollectionTable(name = "hospital_specializations", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "specialization")
    private java.util.List<String> specializations;

    @Column(name = "sepsis_mortality_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal sepsisMortalityRate = BigDecimal.ZERO;

    @Column(name = "is_level1_trauma")
    @Builder.Default
    private Boolean isLevel1Trauma = false;

    @Column(name = "dispatcher_phone", length = 20)
    private String dispatcherPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    @Column(name = "has_api_integration")
    @Builder.Default
    private Boolean hasApiIntegration = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private Double distanceKm;

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
