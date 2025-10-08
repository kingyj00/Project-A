package com.ll.P_A.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    @Schema(description = "Access 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    private final String accessToken;

    @Schema(description = "Refresh 토큰(JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    private final String refreshToken;
}