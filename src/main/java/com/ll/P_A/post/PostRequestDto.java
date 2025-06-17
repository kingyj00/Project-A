package com.ll.P_A.post;

public record PostRequestDto(
        String title,
        String content,
        String author
) {}