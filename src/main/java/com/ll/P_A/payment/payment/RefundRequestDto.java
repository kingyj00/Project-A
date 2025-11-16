package com.ll.P_A.payment.payment;

public record RefundRequestDto(
        String paymentKey,
        String reason
) { }