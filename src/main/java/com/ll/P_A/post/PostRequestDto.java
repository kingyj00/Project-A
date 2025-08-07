package com.ll.P_A.post;

import jakarta.validation.constraints.NotBlank;

public record PostRequestDto(

        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        String author // 현재는 서버 측에서 유저 정보로 설정되므로 유효성 검사 대상 아님
) { }