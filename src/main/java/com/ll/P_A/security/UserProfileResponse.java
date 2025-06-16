package com.ll.P_A.security;

import lombok.Getter;

@Getter
public class UserProfileResponse {
    private final String username;
    private final String email;
    private final boolean emailVerified;

    public UserProfileResponse(String username, String email, boolean emailVerified) {
        this.username = username;
        this.email = email;
        this.emailVerified = emailVerified;
    }
}