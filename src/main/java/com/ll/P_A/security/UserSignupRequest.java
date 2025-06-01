package com.ll.P_A.security;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignupRequest {
    private String username;
    private String password;
    private String nickname;
    private String email;
}