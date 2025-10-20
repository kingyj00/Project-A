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

    private final PaymentRepository paymentRepository; // 결제 데이터 다루는 창구
    private final OrderRepository orderRepository;     // 주문 데이터 다루는 창구

    //결제 시작 단계(멱등키 사용)
    @Transactional
    public Payment initiatePayment(Long orderId, String provider, String method, String idempotencyKey) {
        // 1)키 중복이면 바로 거절 (같은 결제 재요청 방지)
        paymentRepository.findByIdempotencyKey(idempotencyKey).ifPresent(p -> {
            throw new IllegalStateException("이미 처리된 결제 요청(Idempotency-Key 중복): " + idempotencyKey);
        });

        // 2) 주문이 실제로 있는지 확인
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // 3) 결제 가능한 주문 상태인지 확인
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 가능한 주문 상태가 아님: " + order.getStatus());
        }

        // 4) 결제 엔티티 생성 (누가, 얼마를, 어떤 PG/수단으로 결제 시도 중인지 기록)
        Payment payment = Payment.builder()
                .order(order)                       // 어떤 주문의 결제인지
                .amount(order.getAmount())          // 결제 금액
                .provider(provider)                  // PG사(토스, 카카오 등)
                .method(method)                      // 결제수단(카드, 계좌, 간편결제 등)
                .idempotencyKey(idempotencyKey)     // 키 저장
                .status(PaymentStatus.INITIATED)    // 결제 시작 상태로 표시
                .build();

        // 5) DB에 저장 후 반환
        return paymentRepository.save(payment);
    }

    /**
     * 결제 성공 콜백 가정
     * - PG로부터 “성공” 응답이 왔다고 가정하고 처리
     * - 결제 상태를 성공으로 바꾸고, 주문도 “결제완료(PAID)”로 바꿈
     */
    @Transactional
    public void succeedPayment(Long paymentId, String transactionIdFromPg) {
        // 1) 결제 레코드 찾기
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        // 2) 결제 성공으로 표시 + PG에서 받은 거래번호 기록
        payment.markSucceeded(transactionIdFromPg);

        // 3) 연결된 주문도 “결제완료” 상태로 변경
        payment.getOrder().markPaid();
    }

    /**
     * 결제 실패 처리
     * - 왜 실패했는지 코드/메시지 함께 저장
     */
    @Transactional
    public void failPayment(Long paymentId, String failureCode, String failureMessage) {
        // 1) 결제 레코드 찾기
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        // 2) 결제 실패 상태로 변경(사유 기록)
        payment.markFailed(failureCode, failureMessage);
    }

    /**
     * 전액 환불 처리(단순 버전)
     * - 부분 환불을 하려면 “누적 환불액” 같은 필드를 Payment에 추가해서 관리해야 함
     * - 결제를 환불로 표시하고, 주문도 환불 상태로 변경
     */
    @Transactional
    public void refundSucceeded(Long paymentId) {
        // 1) 결제 레코드 찾기
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        // 2) 결제 환불 완료로 표시
        payment.markRefunded();

        // 3) 주문도 환불 상태로 변경
        payment.getOrder().markRefunded();
    }
}