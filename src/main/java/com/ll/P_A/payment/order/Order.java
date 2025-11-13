package com.ll.P_A.payment.order;

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
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 구매자, 상품 정보
    private Long buyerId;
    private Long listingId;

    // 결제용 상품명 필드 추가 (임시 주문용)
    private String name;

    // 결제 금액 (원 단위)
    private Long amount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Embedded
    private ShippingInfo shippingInfo;

    @Version
    private Long version;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ====== 상태 전이 메서드들 ======

    public void markPaid() {
        ensureStatus(OrderStatus.PENDING);
        this.status = OrderStatus.PAID;
    }

    public void markShipped() {
        ensureStatus(OrderStatus.PAID);
        this.status = OrderStatus.SHIPPED;
    }

    public void markCompleted() {
        ensureStatus(OrderStatus.SHIPPED);
        this.status = OrderStatus.COMPLETED;
    }

    public void cancelBeforePayment(String reason) {
        ensureStatus(OrderStatus.PENDING);
        this.status = OrderStatus.CANCELLED;
    }

    public void requestRefund() {
        if (this.status != OrderStatus.PAID && this.status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("환불 요청 불가: " + this.status);
        }
        this.status = OrderStatus.REFUND_REQUESTED;
    }

    public void markRefunded() {
        if (this.status != OrderStatus.REFUND_REQUESTED && this.status != OrderStatus.PAID) {
            throw new IllegalStateException("환불 완료 불가: " + this.status);
        }
        this.status = OrderStatus.REFUNDED;
    }

    private void ensureStatus(OrderStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException("상태 전이 불가: 현재=" + this.status + ", 기대=" + expected);
        }
    }
}