package com.ll.P_A.payment.payment;

public record PaymentRequestDto(
        String provider,        // "MOCK"
        String method,          // "CARD" 등
        String idempotencyKey   // 중복 클릭 방지 키(고유)
) { }