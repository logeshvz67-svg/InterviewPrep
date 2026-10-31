package com.codequest.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;

/**
 * Type-safe view of the "app.*" settings in application.yml.
 * WHY: instead of sprinkling @Value("${...}") everywhere, one record holds all settings,
 * and @Validated makes the app refuse to start if a setting is missing or wrong.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String defaultTimezone,
        boolean registrationEnabled,
        String adminEmails,
        @NotBlank String corsAllowedOrigins,
        Jwt jwt
) {
    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
            @Min(1) int accessTokenMinutes,
            @Min(1) int refreshTokenDays
    ) {}

    public List<String> adminEmailList() {
        if (adminEmails == null || adminEmails.isBlank()) return List.of();
        return Arrays.stream(adminEmails.split(",")).map(s -> s.trim().toLowerCase()).filter(s -> !s.isEmpty()).toList();
    }

    public List<String> corsOriginList() {
        return Arrays.stream(corsAllowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
