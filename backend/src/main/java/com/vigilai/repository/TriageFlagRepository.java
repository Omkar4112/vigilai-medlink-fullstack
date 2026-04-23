package com.vigilai.repository;

import com.vigilai.model.TriageFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TriageFlagRepository extends JpaRepository<TriageFlag, Long> {
    List<TriageFlag> findByPatient_PatientId(Long patientId);
    List<TriageFlag> findByRuleSeverity(String severity);
}
