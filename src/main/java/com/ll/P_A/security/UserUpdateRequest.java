package com.ll.P_A.security;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    private String nickname;
    private String password; // 닉네임과 비번만 변경 가능
}