package com.ll.P_A.payment.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(Long buyerId, Long listingId, Long amount, ShippingInfo ship) {
        // TODO: listing 재고/자기물건 구매 방지 검증(추후 확장)
        Order order = Order.builder()
                .buyerId(buyerId)
                .listingId(listingId)
                .amount(amount)
                .shippingInfo(ship)
                .status(OrderStatus.PENDING)
                .build();
        return orderRepository.save(order);
    }

    @Transactional
    public void markOrderPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.markPaid();
    }

    @Transactional
    public void markOrderShipped(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.markShipped();
    }

    @Transactional
    public void markOrderCompleted(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.markCompleted();
    }

    @Transactional
    public void cancelBeforePayment(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.cancelBeforePayment(reason);
    }

    @Transactional
    public void requestRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.requestRefund();
    }

    @Transactional
    public void markRefunded(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.markRefunded();
    }
}