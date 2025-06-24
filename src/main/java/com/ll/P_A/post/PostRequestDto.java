package com.ll.P_A.post;

import jakarta.validation.constraints.NotBlank;

public record PostRequestDto(

        @NotBlank(message = "제목은 비어 있을 수 없습니다.")
        String title,

        @NotBlank(message = "내용은 비어 있을 수 없습니다.")
        String content,

        String author // 현재는 서버 측에서 유저 정보로 설정되므로 유효성 검사 대상 아님
) { }