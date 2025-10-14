package com.ll.P_A.payment.payment;

public enum PaymentStatus {
    INITIATED,  // 결제 요청 시작
    SUCCEEDED,  // 결제 성공
    FAILED,     // 결제 실패
    REFUNDED    // 환불 완료
}