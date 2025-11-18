package com.ll.P_A.payment.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 주문 ID로 결제 조회
    Optional<Payment> findByOrderId(Long orderId);

    // paymentKey(Toss transaction key)로 조회
    Optional<Payment> findByPaymentKey(String paymentKey);

    // 기반 중복 결제 차단용
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}