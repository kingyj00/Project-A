package com.ll.P_A.payment.payment;

import com.ll.P_A.payment.order.Order;
import com.ll.P_A.payment.order.OrderRepository;
import com.ll.P_A.payment.order.OrderStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    //결제 시작(멱등키 확보) — 다음 단계에서 Mock/PG 클라이언트 연결
    @Transactional
    public Payment initiatePayment(Long orderId, String provider, String method, String idempotencyKey) {
        paymentRepository.findByIdempotencyKey(idempotencyKey).ifPresent(p -> {
            throw new IllegalStateException("이미 처리된 결제 요청(Idempotency-Key 중복): " + idempotencyKey);
        });

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 가능한 주문 상태가 아님: " + order.getStatus());
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getAmount())
                .provider(provider)
                .method(method)
                .idempotencyKey(idempotencyKey)
                .status(PaymentStatus.INITIATED)
                .build();

        return paymentRepository.save(payment);
    }

    //성공 콜백 가정
    @Transactional
    public void succeedPayment(Long paymentId, String transactionIdFromPg) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        payment.markSucceeded(transactionIdFromPg);
        payment.getOrder().markPaid();
    }

    // 실패 처리
    @Transactional
    public void failPayment(Long paymentId, String failureCode, String failureMessage) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        payment.markFailed(failureCode, failureMessage);
    }

    // 전액 환불(단순) — 부분환불은 누적 환불액 필드 추가 필요
    @Transactional
    public void refundSucceeded(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        payment.markRefunded();
        payment.getOrder().markRefunded();
    }
}