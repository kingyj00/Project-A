package com.ll.P_A.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSignupRequest {

    @Schema(description = "로그인 아이디", example = "june11")
    @NotBlank
    @Size(min = 4, max = 20)
    private String username;

    @Schema(description = "로그인 비밀번호", example = "P@ssw0rd!23")
    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @Schema(description = "닉네임", example = "준이")
    @NotBlank
    @Size(min = 2, max = 20)
    private String nickname;

    @Schema(description = "이메일", example = "june11@example.com")
    @NotBlank
    @Email
    private String email;
}