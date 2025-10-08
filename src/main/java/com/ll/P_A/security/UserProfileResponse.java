package com.ll.P_A.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {

    @Schema(description = "아이디", example = "june11")
    private final String username;

    @Schema(description = "이메일", example = "june11@example.com")
    private final String email;

    @Schema(description = "이메일 인증 여부", example = "true")
    private final boolean emailVerified;
}