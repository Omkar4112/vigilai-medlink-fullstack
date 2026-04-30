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

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    public ResponseEntity<?> searchPatients(@RequestParam String clinicId, @RequestParam String query) {
        return ResponseEntity.ok(patientRepo.searchPatients(clinicId, query));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLINIC','ADMIN')")
    public ResponseEntity<?> deletePatient(@PathVariable Long id) {
        if (!patientRepo.existsById(id)) return ResponseEntity.notFound().build();
        patientRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Patient deleted successfully", "id", id));
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
        int total = h.getTotalIcuBeds() != null ? h.getTotalIcuBeds() : 0;
        int occ   = h.getOccupiedBeds() != null ? h.getOccupiedBeds() : 0;
        int avail = Math.max(0, total - occ);

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

    @PostMapping("/doctors")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> addDoctor(@RequestBody Doctor doctor) {
        if (doctor.getHospitalId() == null) return ResponseEntity.badRequest().body("hospitalId is required");
        doctor.setIsAvailable(true);
        Doctor saved = doctorRepo.save(doctor);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/doctors/{id}")
    @PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        if (!doctorRepo.existsById(id)) return ResponseEntity.notFound().build();
        doctorRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Doctor deleted successfully", "id", id));
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
    private final PatientRepository  patientRepo;
    private final DoctorRepository   doctorRepo;
    private final AuditLogService    auditLog;

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminDashboard() {
        Map<String, Object> resp = new LinkedHashMap<>();
        
        long totalUsers     = userRepo.count();
        long totalHospitals = hospitalRepo.count();
        long totalPatients  = patientRepo.count();
        long totalDoctors   = doctorRepo.count();
        long totalAlerts    = alertRepo.count();
        long activeAlerts   = alertRepo.countByStatus("NEW");
        long pendingAlerts  = alertRepo.countByClinicianDecision("PENDING");
        
        // ICU & Doctor Availability Aggregates
        Integer icuTotal    = hospitalRepo.sumTotalIcuBeds();
        Integer icuOccupied = hospitalRepo.sumOccupiedBeds();
        long docsOnDuty     = doctorRepo.countByIsAvailableTrue();

        resp.put("totalUsers",     totalUsers);
        resp.put("totalHospitals", totalHospitals);
        resp.put("totalPatients",  totalPatients);
        resp.put("totalDoctors",   totalDoctors);
        resp.put("totalAlerts",    totalAlerts);
        resp.put("activeAlerts",   activeAlerts);
        resp.put("pendingAlerts",  pendingAlerts);
        resp.put("icuAvailable",   (icuTotal != null ? icuTotal : 0) - (icuOccupied != null ? icuOccupied : 0));
        resp.put("doctorsOnDuty",  docsOnDuty);
        resp.put("systemStatus",   "OPERATIONAL");
        resp.put("lastUpdated",    java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/hospitals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createHospital(@RequestBody Hospital hospital) {
        if (hospital.getCode() == null) hospital.setCode("HOSP-" + System.currentTimeMillis() % 10000);
        Hospital saved = hospitalRepo.save(hospital);
        auditLog.logAction("HOSPITAL_REGISTERED", "ADMIN", String.valueOf(saved.getHospitalId()), "ADMIN_USER", null, saved.getName());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/hospitals/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteHospital(@PathVariable Long id) {
        if (!hospitalRepo.existsById(id)) return ResponseEntity.notFound().build();
        hospitalRepo.deleteById(id);
        auditLog.logAction("HOSPITAL_DELETED", "ADMIN", String.valueOf(id), "ADMIN_USER", null, "ID: " + id);
        return ResponseEntity.ok(Map.of("message", "Hospital unregistered successfully", "id", id));
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
