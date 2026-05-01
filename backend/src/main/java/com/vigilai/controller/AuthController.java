package com.vigilai.controller;

import com.vigilai.config.JwtUtil;
import com.vigilai.dto.AuthDTOs.*;
import com.vigilai.model.Role;
import com.vigilai.model.User;
import com.vigilai.repository.UserRepository;
import com.vigilai.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AuditLogService auditLog;

    /** POST /auth/login */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is disabled"));
        }

        user.setLastLogin(LocalDateTime.now());

        // Auto-assign entityId if missing (fixes null clinicId/hospitalId in frontend)
        if (user.getEntityId() == null || user.getEntityId().isBlank()) {
            switch (user.getRole()) {
                case CLINIC   -> user.setEntityId("clinic-demo-001");
                case HOSPITAL -> user.setEntityId("1");
                default -> {} // ADMIN doesn't need entityId
            }
        }
        userRepo.save(user);

        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole(), user.getEntityId(), user.getId());

        auditLog.logAction("USER_LOGIN", "USER", user.getId().toString(),
                user.getEmail(), null, "Login successful");

        log.info("Login: {} [{}]", user.getEmail(), user.getRole());

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getRole().name(),
                user.getEntityId(),
                user.getFullName(),
                user.getEmail()
        ));
    }

    /** POST /auth/register (Admin only in production) */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        Role role = req.getRole() != null ? req.getRole() : Role.CLINIC;

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .fullName(req.getFullName())
                .entityId(req.getEntityId())
                .phone(req.getPhone())
                .isActive(true)
                .build();

        userRepo.save(user);
        auditLog.logAction("USER_REGISTERED", "USER", user.getEmail(),
                "SYSTEM", null, "role=" + role);

        return ResponseEntity.ok(Map.of("message", "User registered successfully", "role", role));
    }

    /** GET /auth/me — returns current user info from token */
    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email    = jwtUtil.extractEmail(token);
        String role     = jwtUtil.extractRole(token);
        String entityId = jwtUtil.extractEntityId(token);

        return ResponseEntity.ok(Map.of(
                "email", email,
                "role", role,
                "entityId", entityId != null ? entityId : ""
        ));
    }
}
