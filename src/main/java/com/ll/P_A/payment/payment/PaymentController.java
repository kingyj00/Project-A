package com.ll.P_A.payment.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment 도메인 컨트롤러
 * - 모의 결제 해피패스: 주문 결제 시도 → 즉시 성공 처리 → 최신 Payment 상태 응답
 * - 환불(전액) 처리: 성공 처리 후 최신 상태 응답
 * - Toss 테스트 결제 승인 처리: Toss 위젯 결제 완료 후 승인 API 호출
 *
 * URL 규칙:
 *  - POST /api/pay/orders/{orderId}/pay            : 결제 시도(모의 성공)
 *  - POST /api/pay/payments/{paymentId}/refund     : 환불(모의 성공)
 *  - POST /api/pay/toss/confirm                    : Toss 결제 승인 (테스트용)
 */

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // === [1] 기존 모의 결제 처리 ===
    @PostMapping("/orders/{orderId}/pay")
    public PaymentResponseDto pay(@PathVariable Long orderId,
                                  @RequestBody PaymentRequestDto req) {
        // 1) 결제 시작(INITIATED 저장, 멱등키 검증)
        Payment p = paymentService.initiatePayment(
                orderId,
                req.provider(),
                req.method(),
                req.idempotencyKey()
        );

        // 2) 모의 성공 처리(실 PG에선 Toss 승인 API로 대체됨)
        paymentService.succeedPayment(p.getId(), "MOCK-TX-" + p.getId());

        // 3) 최신 상태로 재조회하여 응답(SUCCEEDED + transactionId 반영)
        Payment updated = paymentService.get(p.getId());
        return PaymentResponseDto.from(updated);
    }

    // === [2] 기존 환불 처리 ===
    @PostMapping("/payments/{paymentId}/refund")
    public PaymentResponseDto refund(@PathVariable Long paymentId) {
        // 1) 환불 성공 처리
        paymentService.refundSucceeded(paymentId);

        // 2) 최신 상태로 재조회하여 응답(REFUNDED)
        Payment updated = paymentService.get(paymentId);
        return PaymentResponseDto.from(updated);
    }

    // === [3] Toss 테스트 결제 승인 추가 ===
    /**
     * Toss 결제 위젯에서 결제 완료 시 호출되는 엔드포인트
     * 프론트엔드에서 paymentKey, orderId, amount를 전달받아 Toss 서버로 승인 요청 보냄
     * (실제 돈은 빠져나가지 않음 — 테스트 환경)
     */
    @PostMapping("/toss/confirm")
    public String confirmTossPayment(@RequestBody Map<String, Object> body) {
        try {
            // 프론트에서 받은 데이터 꺼내기
            String paymentKey = (String) body.get("paymentKey");
            String orderId = (String) body.get("orderId");
            int amount = Integer.parseInt(body.get("amount").toString());

            // Toss 결제 승인 요청
            paymentService.confirmTossPayment(paymentKey, orderId, amount);

            // 성공 메시지 반환 (테스트용)
            return "Toss 결제 승인 요청 성공!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Toss 결제 승인 중 오류 발생: " + e.getMessage();
        }
    }
}