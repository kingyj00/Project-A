package com.ll.P_A.payment.order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service                 // 스프링 서비스 레이어 컴포넌트
@RequiredArgsConstructor // final 필드 생성자를 롬복이 자동 생성 (DI 주입)
public class OrderService {

    private final OrderRepository orderRepository; // 주문 영속화/조회 담당 리포지토리

    @Transactional
    public Order createOrder(Long buyerId, Long listingId, Long amount, ShippingInfo ship) {
        // TODO: listing 재고 확인, 본인 상품 구매 방지 등 사전 검증 로직 추가 예정
        // 신규 주문을 PENDING 상태로 생성하고 저장
        Order order = Order.builder()
                .buyerId(buyerId)         // 구매자 식별자
                .listingId(listingId)     // 상품(리스팅) 식별자
                .amount(amount)           // 결제 금액(원)
                .shippingInfo(ship)       // 배송지/연락처 등 배송 정보 값 객체
                .status(OrderStatus.PENDING) // 초기 상태: 결제 대기
                .build();
        return orderRepository.save(order); // DB에 저장 후 영속 엔티티 반환
    }

    @Transactional
    public void markOrderPaid(Long orderId) {
        // 주문 조회(없으면 IllegalArgumentException 발생)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.markPaid(); // 도메인 메서드: PENDING -> PAID 등 상태 전이와 검증 캡슐화
    }

    @Transactional
    public void markOrderShipped(Long orderId) {
        // 결제 완료된 주문을 배송중 상태로 전환
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.markShipped(); // 도메인 규칙 내부에서 상태/시간 필드 업데이트
    }

    @Transactional
    public void markOrderCompleted(Long orderId) {
        // 배송 완료 후 구매 확정(완료) 상태로 전환
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.markCompleted(); // 환불/취소 불가 등 비즈니스 규칙 확인은 엔티티 내에서 처리
    }

    @Transactional
    public void cancelBeforePayment(Long orderId, String reason) {
        // 결제 전(또는 허용 상태)에서의 주문 취소 처리 + 사유 기록
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.cancelBeforePayment(reason); // 상태 전이 & 취소사유 세팅
    }

    @Transactional
    public void requestRefund(Long orderId) {
        // 결제/배송 조건에 따라 환불 요청 상태로 전환
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.requestRefund(); // 환불 가능 여부 검증, 요청 시각 기록 등 도메인 로직
    }

    @Transactional
    public void markRefunded(Long orderId) {
        // 환불 완료 처리(실제 PG 환불 성공 콜백 이후 호출되는 시나리오)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.markRefunded(); // 금액/상태/환불일시 업데이트, 중복 처리 방지 검증 포함
    }
}