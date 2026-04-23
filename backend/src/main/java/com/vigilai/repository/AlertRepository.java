package com.vigilai.repository;

import com.vigilai.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByClinicIdOrderByAlertTimestampDesc(String clinicId);
    List<Alert> findBySeverityOrderByAlertTimestampDesc(String severity);
    List<Alert> findByClinicianDecisionOrderByAlertTimestampDesc(String decision);
    List<Alert> findByPatient_PatientIdOrderByAlertTimestampDesc(Long patientId);
    List<Alert> findByHospitalIdOrderByAlertTimestampDesc(Long hospitalId);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.clinicId = :cid AND a.clinicianDecision = 'PENDING'")
    long countPendingByClinic(@Param("cid") String clinicId);
}
