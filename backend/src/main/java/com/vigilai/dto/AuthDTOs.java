package com.vigilai.dto;

import com.vigilai.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// ── Auth ─────────────────────────────────────────────────────
public class AuthDTOs {

    @Data
    public static class LoginRequest {
        @Email  @NotBlank public String email;
        @NotBlank         public String password;
    }

    @Data
    public static class RegisterRequest {
        @Email  @NotBlank public String email;
        @NotBlank         public String password;
        @NotBlank         public String fullName;
        public Role       role;
        public String     entityId;
        public String     phone;
    }

    @Data
    public static class AuthResponse {
        public String token;
        public String role;
        public String entityId;
        public String fullName;
        public String email;

        public AuthResponse(String token, String role, String entityId, String fullName, String email) {
            this.token    = token;
            this.role     = role;
            this.entityId = entityId;
            this.fullName = fullName;
            this.email    = email;
        }
    }
}
