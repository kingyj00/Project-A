package com.ll.P_A.payment.payment;

public record TossConfirmRequestDto(
        String paymentKey,
        String orderId,
        int amount
) { }