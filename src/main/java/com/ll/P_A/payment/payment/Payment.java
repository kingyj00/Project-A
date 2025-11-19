package com.ll.P_A.payment.payment;

import com.ll.P_A.payment.order.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_order", columnList = "order_id"),
                @Index(name = "idx_payment_paymentKey", columnList = "paymentKey", unique = true),
                @Index(name = "idx_payment_txid", columnList = "transactionId", unique = true),
                @Index(name = "idx_payment_idempotency", columnList = "idempotencyKey", unique = true)
        }
)
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id")
    private Order order;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String provider;
    private String method;

    // Toss 결제에서 필수: paymentKey(환불/조회/승인)
    @Column(unique = true)
    private String paymentKey;

    // PG 내부 특성상 필요한 고유 트랜잭션(=결제승인ID)
    private String transactionId;

    @Column(unique = true)
    private String idempotencyKey;

    private String failureCode;
    private String failureMessage;

    @Version
    private Long version;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /* ===== 도메인 메서드 ===== */
    public void markSucceeded(String paymentKeyFromPg) {
        ensureStatus(PaymentStatus.INITIATED);
        this.status = PaymentStatus.SUCCEEDED;

        // Toss 기준 paymentKey 저장
        this.paymentKey = paymentKeyFromPg;

        // 트랜잭션 ID: paymentKey를 그대로 쓰거나
        if (this.transactionId == null) {
            this.transactionId = paymentKeyFromPg;
        }
    }

    public void markFailed(String code, String message) {
        if (this.status == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("이미 성공한 결제를 실패로 바꿀 수 없음");
        }
        this.status = PaymentStatus.FAILED;
        this.failureCode = code;
        this.failureMessage = message;
    }

    public void markRefunded() {
        if (this.status != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("성공한 결제만 환불 가능");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    private void ensureStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException("결제 상태 전이 불가: 현재=" + this.status + ", 기대=" + expected);
        }
    }
}