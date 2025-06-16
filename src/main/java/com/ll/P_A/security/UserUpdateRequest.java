package com.ll.P_A.security;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    private String nickname;
    private String email;            //  이메일 필드 추가
    private String password;         //  변경할 비밀번호 (서비스에 맞춤)
}