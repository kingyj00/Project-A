package com.ll.P_A.security;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    private String nickname;
    private String currentPassword;  // 추가: 기존 비밀번호
    private String newPassword;      // 변경할 비밀번호
}