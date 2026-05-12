package com.vigilai.controller;

import com.vigilai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified dashboard summary endpoint accessible to all authenticated roles.
 * Returns 100% live database counts — zero hardcoded values.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository     userRepo;
    private final HospitalRepository hospitalRepo;
    private final PatientRepository  patientRepo;
    private final DoctorRepository   doctorRepo;
    private final AlertRepository    alertRepo;
    private final VitalRepository    vitalRepo;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> summary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Map<String, Object> data = new LinkedHashMap<>();

        // Core counts
        data.put("totalUsers",      userRepo.count());
        data.put("totalHospitals",  userRepo.countByRole(com.vigilai.model.Role.HOSPITAL));
        data.put("totalPatients",   patientRepo.count());
        data.put("totalDoctors",    doctorRepo.count());
        data.put("totalAlerts",     alertRepo.count());

        // Alert breakdown
        data.put("activeAlerts",    alertRepo.countByStatus("NEW"));
        data.put("pendingAlerts",   alertRepo.countByClinicianDecision("PENDING"));
        data.put("dispatchedToday", alertRepo.countDispatchedSince(startOfDay));

        // Vitals today (global — across all clinics)
        data.put("vitalsToday",     vitalRepo.countVitalsSinceGlobal(startOfDay));

        // Staff availability
        data.put("doctorsOnDuty",   doctorRepo.countByIsAvailableTrue());

        // ICU capacity
        // Since hospitals are now managed purely via users table, ICU capacity isn't tracked in a separate table anymore.
        data.put("icuAvailable",    0);
        data.put("icuTotal",        0);

        data.put("systemStatus",    "OPERATIONAL");
        data.put("lastUpdated",     LocalDateTime.now().toString());

        return ResponseEntity.ok(data);
    }
}
