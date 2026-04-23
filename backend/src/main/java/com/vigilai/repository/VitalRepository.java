package com.vigilai.repository;

import com.vigilai.model.Vital;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VitalRepository extends JpaRepository<Vital, Long> {
    List<Vital> findByPatient_PatientIdOrderByVitalTimestampDesc(Long patientId);
    List<Vital> findByClinicIdAndSyncStatusOrderByCreatedAtDesc(String clinicId, String syncStatus);
    List<Vital> findBySyncStatus(String syncStatus);

    @Query("SELECT v FROM Vital v WHERE v.patient.patientId = :pid ORDER BY v.vitalTimestamp DESC")
    List<Vital> findPatientHistory(@Param("pid") Long patientId);

    @Query("SELECT COUNT(v) FROM Vital v WHERE v.clinicId = :cid AND v.syncStatus = :status")
    long countByClinicAndStatus(@Param("cid") String clinicId, @Param("status") String status);

    @Query("SELECT COUNT(v) FROM Vital v WHERE v.clinicId = :cid AND v.vitalTimestamp >= :since")
    long countVitalsSince(@Param("cid") String clinicId, @Param("since") java.time.LocalDateTime since);
}
