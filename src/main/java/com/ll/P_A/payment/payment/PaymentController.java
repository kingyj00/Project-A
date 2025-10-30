package com.ll.P_A.payment.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Payment 도메인 컨트롤러
 * - 모의 결제 해피패스: 주문 결제 시도 → 즉시 성공 처리 → 최신 Payment 상태 응답
 * - 환불(전액) 처리: 성공 처리 후 최신 상태 응답
 *
 * URL 규칙:
 *  - POST /api/pay/orders/{orderId}/pay      : 결제 시도(모의 성공)
 *  - POST /api/pay/payments/{paymentId}/refund : 환불(모의 성공)
 */

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

   // 결제 시도
    @PostMapping("/orders/{orderId}/pay")
    public PaymentResponseDto pay(@PathVariable Long orderId,
                                  @RequestBody PaymentRequestDto req) {
        // 1) 결제 시작(INITIATED 저장, 멱등키 검증)
        // 추후 토스 결제 테스트용으로 변경
        Payment p = paymentService.initiatePayment(
                orderId,
                req.provider(),
                req.method(),
                req.idempotencyKey()
        );

        // 2) 모의 성공 처리(실 PG에선 웹훅으로 대체)
        paymentService.succeedPayment(p.getId(), "MOCK-TX-" + p.getId());

        // 3) 최신 상태로 재조회하여 응답(SUCCEEDED + transactionId 반영)
        Payment updated = paymentService.get(p.getId());
        return PaymentResponseDto.from(updated);
    }

    // 환불 처리
    @PostMapping("/payments/{paymentId}/refund")
    public PaymentResponseDto refund(@PathVariable Long paymentId) {
        // 1) 환불 성공 처리
        paymentService.refundSucceeded(paymentId);

        // 2) 최신 상태로 재조회하여 응답(REFUNDED)
        Payment updated = paymentService.get(paymentId);
        return PaymentResponseDto.from(updated);
    }
}