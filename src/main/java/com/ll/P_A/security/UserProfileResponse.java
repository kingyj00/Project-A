package com.ll.P_A.security;

import lombok.Getter;

@Getter
public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private boolean enabled;
    private boolean isAdmin;

    public UserProfileResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.enabled = user.isEnabled();
        this.isAdmin = user.isAdmin();
    }
}