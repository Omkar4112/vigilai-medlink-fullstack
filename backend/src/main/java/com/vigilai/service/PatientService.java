package com.vigilai.service;

import com.vigilai.model.Patient;
import com.vigilai.repository.PatientRepository;
import com.vigilai.repository.VitalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PatientService {

    @Autowired private PatientRepository patientRepo;
    @Autowired private VitalRepository   vitalRepo;

    public Patient findOrCreate(String clinicId, String phone, Integer age,
                                 String gender, String fullName) {
        return patientRepo.findByClinicIdAndPhoneNumber(clinicId, phone)
                .orElseGet(() -> {
                    Patient p = Patient.builder()
                            .clinicId(clinicId).phoneNumber(phone)
                            .age(age != null ? age : 0)
                            .gender(gender).fullName(fullName).build();
                    Patient saved = patientRepo.save(p);
                    log.info("New patient id={} clinic={}", saved.getPatientId(), clinicId);
                    return saved;
                });
    }

    public Patient getById(Long id) {
        return patientRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + id));
    }

    public Map<String, Object> getDashboardStats(String clinicId) {
        long total   = patientRepo.countByClinicId(clinicId);
        long pending = vitalRepo.countByClinicAndStatus(clinicId, "PENDING");
        long synced  = vitalRepo.countByClinicAndStatus(clinicId, "SYNCED");
        return Map.of(
                "totalPatients", total,
                "pendingSync",   pending,
                "syncedCount",   synced
        );
    }
}
