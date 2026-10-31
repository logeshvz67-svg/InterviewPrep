package com.codequest.dto;

import com.codequest.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO = Data Transfer Object: the exact JSON shape the API accepts/returns.
 * WHY not return entities directly: entities may hold secrets (password hash), lazy fields
 * (LazyInitializationException while writing JSON) and change shape with the DB.
 * Validation annotations are checked when a controller parameter has @Valid.
 */
public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password,
            @NotBlank @Size(max = 80) String displayName) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UserView(Long id, String email, String displayName, Role role) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds, UserView user) {}

    public record AuthConfig(boolean registrationEnabled) {}
}
