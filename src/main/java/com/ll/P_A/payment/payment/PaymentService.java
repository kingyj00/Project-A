package com.ll.P_A.payment.payment;

import com.ll.P_A.payment.order.Order;
import com.ll.P_A.payment.order.OrderRepository;
import com.ll.P_A.payment.order.OrderStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository; // 결제 데이터 다루는 창구
    private final OrderRepository orderRepository;     // 주문 데이터 다루는 창구

    // Toss SecretKey (application.yml에서 자동 주입)
    @Value("${toss.secret-key}")
    private String tossSecretKey;

    // Toss API 결제 승인 주소
    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    // 단건 조회 (읽기 전용)
    @Transactional(Transactional.TxType.SUPPORTS)
    public Payment get(Long paymentId) {
        return getOrThrow(paymentId);
    }

    // 결제 시작 단계 (멱등키 사용)
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

    // Toss 결제 승인 요청 (테스트용)
    @Transactional
    public void confirmTossPayment(String paymentKey, String orderId, int amount) {
        try {
            // Toss API 요청
            JSONObject body = new JSONObject();
            body.put("paymentKey", paymentKey);
            body.put("orderId", orderId);
            body.put("amount", amount);

            // Toss 인증 헤더
            String encodedAuth = Base64.getEncoder()
                    .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

            // HTTP POST 요청으로 Toss 서버에 결제 승인 요청
            String response = Request.post(TOSS_CONFIRM_URL)
                    .addHeader("Authorization", "Basic " + encodedAuth)
                    .bodyString(body.toString(), ContentType.APPLICATION_JSON)
                    .execute()
                    .returnContent()
                    .asString();

            // Toss 응답 로그 출력 (테스트용)
            System.out.println("Toss 결제 승인 응답: " + response);

        } catch (Exception e) {
            throw new RuntimeException("Toss 결제 승인 요청 실패", e);
        }
    }

    // 결제 성공 처리
    @Transactional
    public void succeedPayment(Long paymentId, String transactionIdFromPg) {
        Payment payment = getOrThrow(paymentId);
        payment.markSucceeded(transactionIdFromPg);
        payment.getOrder().markPaid();
    }

    // 결제 실패 처리
    @Transactional
    public void failPayment(Long paymentId, String failureCode, String failureMessage) {
        Payment payment = getOrThrow(paymentId);
        payment.markFailed(failureCode, failureMessage);
    }

    // 전액 환불 처리
    @Transactional
    public void refundSucceeded(Long paymentId) {
        Payment payment = getOrThrow(paymentId);
        payment.markRefunded();
        payment.getOrder().markRefunded();
    }

    // 공통: 결제 ID로 조회 or 예외
    private Payment getOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }
}