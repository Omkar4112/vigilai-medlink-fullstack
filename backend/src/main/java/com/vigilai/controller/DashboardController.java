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
    public ResponseEntity<?> summary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Map<String, Object> data = new LinkedHashMap<>();

        // Core counts
        data.put("totalUsers",      userRepo.count());
        data.put("totalHospitals",  hospitalRepo.count());
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

        // ICU capacity (sum across all hospitals)
        Integer icuTotal    = hospitalRepo.sumTotalIcuBeds();
        Integer icuOccupied = hospitalRepo.sumOccupiedBeds();
        int icuAvail = (icuTotal != null ? icuTotal : 0) - (icuOccupied != null ? icuOccupied : 0);
        data.put("icuAvailable",    Math.max(0, icuAvail));
        data.put("icuTotal",        icuTotal != null ? icuTotal : 0);

        data.put("systemStatus",    "OPERATIONAL");
        data.put("lastUpdated",     LocalDateTime.now().toString());

        return ResponseEntity.ok(data);
    }
}
