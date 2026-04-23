package com.vigilai.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "triage_flags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TriageFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "triage_id")
    private Long triageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vital_id", nullable = false)
    private Vital vital;

    @Column(name = "rule_severity", nullable = false, length = 20)
    private String ruleSeverity;

    @Column(name = "hr_flag") @Builder.Default   private Boolean hrFlag   = false;
    @Column(name = "temp_flag") @Builder.Default  private Boolean tempFlag  = false;
    @Column(name = "rr_flag") @Builder.Default    private Boolean rrFlag    = false;
    @Column(name = "bp_flag") @Builder.Default    private Boolean bpFlag    = false;
    @Column(name = "spo2_flag") @Builder.Default  private Boolean spo2Flag  = false;

    @Column(name = "flag_count") @Builder.Default private Integer flagCount = 0;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;

    @PrePersist
    protected void onCreate() {
        flaggedAt = LocalDateTime.now();
    }
}
