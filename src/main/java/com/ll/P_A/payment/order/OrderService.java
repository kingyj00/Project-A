package com.ll.P_A.payment.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional(Transactional.TxType.SUPPORTS)
    public Order get(Long orderId) {
        return getOrThrow(orderId);
    }

    @Transactional
    public Order createOrder(Long buyerId, Long listingId, Long amount, ShippingInfo ship) {
        Order order = Order.builder()
                .buyerId(buyerId)
                .listingId(listingId)
                .amount(amount)
                .shippingInfo(ship)
                .status(OrderStatus.PENDING)
                .build();
        return orderRepository.save(order);
    }

    // Toss 결제용 임시 주문 생성
    @Transactional
    public Order createOrderForPayment(String productName, int totalAmount) {
        Order order = Order.builder()
                .buyerId(null)
                .listingId(null)
                .name(productName)
                .amount((long) totalAmount)
                .status(OrderStatus.PENDING)
                .build();
        return orderRepository.save(order);
    }

    @Transactional
    public void markOrderPaid(Long orderId) {
        Order order = getOrThrow(orderId);
        order.markPaid();
    }

    @Transactional
    public void markOrderShipped(Long orderId) {
        Order order = getOrThrow(orderId);
        order.markShipped();
    }

    @Transactional
    public void markOrderCompleted(Long orderId) {
        Order order = getOrThrow(orderId);
        order.markCompleted();
    }

    @Transactional
    public void cancelBeforePayment(Long orderId, String reason) {
        Order order = getOrThrow(orderId);
        order.cancelBeforePayment(reason);
    }

    @Transactional
    public void requestRefund(Long orderId) {
        Order order = getOrThrow(orderId);
        order.requestRefund();
    }

    @Transactional
    public void markRefunded(Long orderId) {
        Order order = getOrThrow(orderId);
        order.markRefunded();
    }

    private Order getOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
}