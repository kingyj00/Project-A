package com.ll_P_A.payment.payment;

import com.ll.P_A.payment.payment.Payment;
import com.ll.P_A.payment.payment.PaymentRequestDto;
import com.ll.P_A.payment.payment.PaymentResponseDto;
import com.ll.P_A.payment.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // [1] 기존 모의 결제 처리
    @PostMapping("/orders/{orderId}/pay")
    public PaymentResponseDto pay(@PathVariable Long orderId,
                                  @RequestBody PaymentRequestDto req) {

        // 1) 결제 시작 (INIIATED)
        Payment p = paymentService.initiatePayment(
                orderId,
                req.provider(),
                req.method(),
                req.idempotencyKey()
        );

        // 2) 모의 성공 처리
        paymentService.succeedPayment(p.getId(), "MOCK-TX-" + p.getId());

        // 3) 최신 데이터 반환
        Payment updated = paymentService.get(p.getId());
        return PaymentResponseDto.from(updated);
    }

    // [2] 기존 모의 환불 처리
    @PostMapping("/payments/{paymentId}/refund")
    public PaymentResponseDto refund(@PathVariable Long paymentId) {
        // 모의 환불 성공 처리
        paymentService.refundSucceeded(paymentId);

        // 최신 상태 응답
        Payment updated = paymentService.get(paymentId);
        return PaymentResponseDto.from(updated);
    }

    // [3] Toss 결제 승인 처리
    //     Toss 위젯 성공 → paymentKey 전달
    @PostMapping("/toss/confirm")
    public ResponseEntity<PaymentResponseDto> confirmToss(@RequestBody TossConfirmRequestDto dto) {

        Payment payment = paymentService.confirmToss(
                dto.paymentKey(),
                dto.orderId(),
                dto.amount()
        );

        return ResponseEntity.ok(PaymentResponseDto.from(payment));
    }

    // [4] Toss 환불 처리 (실환불)
    @PostMapping("/toss/refund")
    public ResponseEntity<PaymentResponseDto> refundToss(@RequestBody RefundRequestDto dto) {

        Payment payment = paymentService.refundToss(
                dto.paymentKey(),
                dto.reason()
        );

        return ResponseEntity.ok(PaymentResponseDto.from(payment));
    }
}