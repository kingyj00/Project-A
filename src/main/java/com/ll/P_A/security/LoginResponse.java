package com.ll.P_A.security;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}