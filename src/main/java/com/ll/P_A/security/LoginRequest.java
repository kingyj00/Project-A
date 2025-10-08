package com.ll.P_A.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "로그인 아이디", example = "june11")
    @NotBlank
    private String username;

    @Schema(description = "로그인 비밀번호", example = "P@ssw0rd!23")
    @NotBlank
    private String password;
}