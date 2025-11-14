package com.ll.P_A.payment.payment;

import com.ll.P_A.payment.order.Order;
import com.ll.P_A.payment.order.OrderRepository;
import com.ll.P_A.payment.order.OrderStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private static final String TOSS_REFUND_URL = "https://api.tosspayments.com/v1/payments/";

    // 단건 조회
    @Transactional(Transactional.TxType.SUPPORTS)
    public Payment get(Long paymentId) {
        return getOrThrow(paymentId);
    }

    // 결제 시작
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

    // Toss 결제 승인(confirm)
    @Transactional
    public Payment confirmToss(String paymentKey, String orderId, int amount) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            JSONObject body = new JSONObject();
            body.put("paymentKey", paymentKey);
            body.put("orderId", orderId);
            body.put("amount", amount);

            String encodedAuth = Base64.getEncoder()
                    .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

            HttpPost post = new HttpPost(TOSS_CONFIRM_URL);
            post.setHeader("Authorization", "Basic " + encodedAuth);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

            ClassicHttpResponse response = (ClassicHttpResponse) client.executeOpen(null, post, null);

            int code = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);

            if (code < 200 || code >= 300) {
                throw new IllegalStateException("TOSS_CONFIRM_FAILED: " + responseBody);
            }

            long oid = Long.parseLong(orderId);

            Order order = orderRepository.findById(oid)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            if (order.getAmount().intValue() != amount) {
                throw new IllegalStateException("금액 불일치: order=" + order.getAmount() + " request=" + amount);
            }

            Payment payment = paymentRepository.findByOrderId(order.getId())
                    .orElse(Payment.builder()
                            .order(order)
                            .amount((long) amount)
                            .provider("TOSS")
                            .method("CARD")
                            .status(PaymentStatus.INITIATED)
                            .build());

            payment.markSucceeded(paymentKey);
            order.markPaid();

            return paymentRepository.save(payment);

        } catch (Exception e) {
            throw new IllegalStateException("TOSS_CONFIRM_FAILED: " + e.getMessage());
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

    // Toss 환불 처리
    @Transactional
    public Payment refundToss(String paymentKey, String reason) {

        String refundUrl = TOSS_REFUND_URL + paymentKey + "/cancel";

        JSONObject body = new JSONObject();
        body.put("cancelReason", reason);

        String encodedAuth = Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost post = new HttpPost(refundUrl);
            post.setHeader("Authorization", "Basic " + encodedAuth);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

            ClassicHttpResponse response = (ClassicHttpResponse) client.executeOpen(null, post, null);

            int code = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);

            if (code < 200 || code >= 300) {
                throw new IllegalStateException("TOSS_REFUND_FAILED: " + responseBody);
            }

            Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            payment.markRefunded();
            payment.getOrder().markRefunded();

            return payment;

        } catch (Exception e) {
            throw new IllegalStateException("TOSS_REFUND_FAILED: " + e.getMessage());
        }
    }

    // 내부 공통
    private Payment getOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }
}