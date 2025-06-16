package com.ll.P_A.security;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    private String nickname;
    private String email;

    private String currentPassword;  //  현재 비밀번호 (검증용)
    private String newPassword;      //  새 비밀번호 (변경 대상)
}