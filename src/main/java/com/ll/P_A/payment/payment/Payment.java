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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String provider;
    private String method;

    // Toss 전용 key (confirm/refund/조회 필수)
    @Column(unique = true)
    private String paymentKey;

    // PG 내부 트랜잭션 ID (= Toss paymentKey 사용 가능)
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

    /* ============================
       결제 상태 전이 도메인 메서드
       ============================ */
    public void markSucceeded(String paymentKeyFromPg) {
        ensureStatus(PaymentStatus.INITIATED);

        this.status = PaymentStatus.SUCCEEDED;

        // ★ Toss paymentKey 저장
        this.paymentKey = paymentKeyFromPg;

        // transactionId가 비어 있으면 paymentKey로 초기화
        if (this.transactionId == null) {
            this.transactionId = paymentKeyFromPg;
        }
    }

    public void markFailed(String code, String message) {
        if (this.status == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("이미 성공한 결제는 실패로 변경할 수 없습니다.");
        }
        this.status = PaymentStatus.FAILED;
        this.failureCode = code;
        this.failureMessage = message;
    }

    public void markRefunded() {
        if (this.status != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("성공한 결제만 환불할 수 있습니다.");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    private void ensureStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException("결제 상태 전이 불가: 현재=" + this.status + ", 기대=" + expected);
        }
    }
}