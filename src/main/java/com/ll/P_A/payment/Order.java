package com.ll.P_A.payment;

import com.ll.P_A.payment.OrderStatus;
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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 우선 ID만 들고가고 추후 연관관계로 확장
    private Long buyerId;
    private Long listingId;

    private Long amount; // 원 단위

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
