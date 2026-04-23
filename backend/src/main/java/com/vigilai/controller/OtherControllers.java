package com.vigilai.controller;

import com.vigilai.model.*;
import com.vigilai.repository.*;
import com.vigilai.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// ── Patient Controller ────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/clinic/patients")
@RequiredArgsConstructor
class PatientController {

    private final PatientRepository patientRepo;
    private final VitalRepository   vitalRepo;
    private final AlertRepository   alertRepo;
    private final PatientService    patientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    public ResponseEntity<?> listPatients(@RequestParam String clinicId) {
        return ResponseEntity.ok(patientRepo.findByClinicId(clinicId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    public ResponseEntity<?> getPatient(@PathVariable Long id) {
        return patientRepo.findById(id).map(p -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("patient",  p);
            result.put("vitals",   vitalRepo.findPatientHistory(p.getPatientId()));
            result.put("alerts",   alertRepo.findByPatient_PatientIdOrderByAlertTimestampDesc(p.getPatientId()));
            result.put("ageGroup", p.getAgeGroup());
            return ResponseEntity.ok((Object) result);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/phone/{phone}")
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    public ResponseEntity<?> getByPhone(@PathVariable String phone) {
        return patientRepo.findByPhoneNumber(phone)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    public ResponseEntity<?> dashboard(@RequestParam String clinicId) {
        return ResponseEntity.ok(patientService.getDashboardStats(clinicId));
    }
}

// ── Hospital Controller ───────────────────────────────────────────────────
@RestController
@RequestMapping("/api/hospital")
@RequiredArgsConstructor
class HospitalController {

    private final HospitalRepository hospitalRepo;
    private final DoctorRepository   doctorRepo;
    private final AlertRepository    alertRepo;
    private final AuditLogService    auditLog;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> dashboard(@RequestParam Long hospitalId) {
        Hospital h = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found: " + hospitalId));

        List<Doctor> onDuty     = doctorRepo.findByHospitalIdAndIsAvailableTrue(hospitalId);
        List<Alert>  dispatched = alertRepo.findByHospitalIdOrderByAlertTimestampDesc(hospitalId);
        List<Alert>  pending    = alertRepo.findByClinicianDecisionOrderByAlertTimestampDesc("PENDING");
        int avail               = h.getTotalIcuBeds() - h.getOccupiedBeds();

        // Use LinkedHashMap to avoid Map.of() 10-key limit
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("hospital",         h);
        resp.put("icuAvailable",     avail);
        resp.put("icuTotal",         h.getTotalIcuBeds());
        resp.put("icuOccupied",      h.getOccupiedBeds());
        resp.put("totalDoctors",     doctorRepo.findByHospitalId(hospitalId).size());
        resp.put("availableDoctors", onDuty.size());
        resp.put("pendingAlerts",    pending.size());
        resp.put("dispatchedAlerts", dispatched.size());
        resp.put("recentAlerts",     dispatched.subList(0, Math.min(10, dispatched.size())));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> doctors(@RequestParam Long hospitalId) {
        return ResponseEntity.ok(doctorRepo.findByHospitalId(hospitalId));
    }

    @PutMapping("/doctors/{id}/availability")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> toggleDoctor(@PathVariable Long id,
                                          @RequestBody Map<String, Boolean> body) {
        return doctorRepo.findById(id).map(d -> {
            d.setIsAvailable(body.getOrDefault("available", d.getIsAvailable()));
            doctorRepo.save(d);
            return ResponseEntity.ok((Object) Map.of("doctorId", id, "available", d.getIsAvailable()));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/icu/beds")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> updateBeds(@RequestParam Long hospitalId,
                                        @RequestBody Map<String, Integer> body) {
        return hospitalRepo.findById(hospitalId).map(h -> {
            if (body.containsKey("occupiedBeds")) h.setOccupiedBeds(body.get("occupiedBeds"));
            if (body.containsKey("totalIcuBeds"))  h.setTotalIcuBeds(body.get("totalIcuBeds"));
            hospitalRepo.save(h);
            auditLog.logAction("ICU_BEDS_UPDATED", "HOSPITAL", String.valueOf(hospitalId),
                    "HOSPITAL_STAFF", null, "occupied=" + h.getOccupiedBeds() + "/" + h.getTotalIcuBeds());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalIcuBeds",  h.getTotalIcuBeds());
            result.put("occupiedBeds",  h.getOccupiedBeds());
            result.put("availableBeds", h.getTotalIcuBeds() - h.getOccupiedBeds());
            return ResponseEntity.ok((Object) result);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}

// ── Admin Controller ──────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
class AdminController {

    private final HospitalRepository hospitalRepo;
    private final UserRepository     userRepo;
    private final AlertRepository    alertRepo;
    private final AuditLogService    auditLog;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminDashboard() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalHospitals", hospitalRepo.count());
        resp.put("totalUsers",     userRepo.count());
        resp.put("totalAlerts",    alertRepo.count());
        resp.put("systemStatus",   "OPERATIONAL");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/hospitals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> allHospitals() {
        return ResponseEntity.ok(hospitalRepo.findAll());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> allUsers() {
        return ResponseEntity.ok(userRepo.findAll());
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditLog.getPagedLogs(page, size));
    }

    @PostMapping("/audit/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verifyIntegrity() {
        boolean intact = auditLog.verifyIntegrity();
        return ResponseEntity.ok(Map.of(
                "integrityVerified", intact,
                "status", intact ? "CHAIN_INTACT" : "BREACH_DETECTED"
        ));
    }
}

// ── Health ────────────────────────────────────────────────────────────────
@RestController
class HealthController {
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status",  "healthy",
                "service", "VigilAI MedLink v2.0",
                "time",    java.time.LocalDateTime.now().toString()
        ));
    }
}
