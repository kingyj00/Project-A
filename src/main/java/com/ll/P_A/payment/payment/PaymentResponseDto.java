package com.ll.P_A.payment.payment;

public record PaymentResponseDto(
        Long id,
        Long orderId,
        Long amount,
        PaymentStatus status,
        String provider,
        String method,
        String transactionId
) {
    public static PaymentResponseDto from(Payment p) {
        return new PaymentResponseDto(
                p.getId(),
                p.getOrder() != null ? p.getOrder().getId() : null,
                p.getAmount(),
                p.getStatus(),
                p.getProvider(),
                p.getMethod(),
                p.getTransactionId()
        );
    }
}