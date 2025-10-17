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

    private String provider;        // "MOCK", "TOSS", "KG", "PORTONE" 등
    private String method;          // "CARD", "VBANK" 등
    private String transactionId;   // PG 거래 ID(고유)
    private String idempotencyKey;  // 중복 결제 방지 키

    private String failureCode;
    private String failureMessage;

    @Version
    private Long version;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /* ===== 도메인 메서드 ===== */
    public void markSucceeded(String transactionIdFromPg) {
        ensureStatus(PaymentStatus.INITIATED);
        this.status = PaymentStatus.SUCCEEDED;
        if (this.transactionId == null) this.transactionId = transactionIdFromPg;
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