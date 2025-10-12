package com.ll.P_A.payment;

public enum OrderStatus {
    PENDING,          // 주문 생성(결제 전)
    PAID,             // 결제 성공
    SHIPPED,          // 발송됨
    COMPLETED,        // 수취확정
    CANCELLED,        // 결제 전 취소
    REFUND_REQUESTED, // 환불 요청
    REFUNDED          // 환불 완료
}