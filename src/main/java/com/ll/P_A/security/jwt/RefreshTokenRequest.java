package com.ll.P_A.security.jwt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "Refresh 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiIs...")
        @NotBlank
        String refreshToken
) {}