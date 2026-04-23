package com.vigilai.repository;

import com.vigilai.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhoneNumber(String phoneNumber);
    Optional<Patient> findByClinicIdAndPhoneNumber(String clinicId, String phoneNumber);
    List<Patient> findByClinicId(String clinicId);
    boolean existsByClinicIdAndPhoneNumber(String clinicId, String phoneNumber);
    long countByClinicId(String clinicId);
}
