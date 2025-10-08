package com.ll.P_A.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Schema(description = "현재 비밀번호(비번 변경 시 필수)", example = "P@ssw0rd!23")
    private String currentPassword;

    @Schema(description = "새 비밀번호", example = "NewP@ssw0rd!45")
    @Size(min = 8, max = 64)
    private String newPassword;

    @Schema(description = "변경할 이메일", example = "new@example.com")
    @Email
    private String email;

    @Schema(description = "변경할 닉네임", example = "준이2")
    @Size(min = 2, max = 20)
    private String nickname;
}