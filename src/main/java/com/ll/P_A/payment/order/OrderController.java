package com.ll.P_A.payment.order;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pay/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 기존 주문 생성 (배송 포함)
    @PostMapping
    public OrderResponseDto create(@RequestBody OrderRequestDto req) {
        var order = orderService.createOrder(
                req.buyerId(),
                req.listingId(),
                req.amount(),
                req.shippingInfo() != null ? req.shippingInfo().toEntity() : null
        );
        return OrderResponseDto.from(order);
    }

    // 기존 주문 조회
    @GetMapping("/{orderId}")
    public OrderResponseDto get(@PathVariable Long orderId) {
        var order = orderService.get(orderId);
        return OrderResponseDto.from(order);
    }

    // 프론트용 주문 생성 (결제 직전)
    @PostMapping("/create")
    public Map<String, Object> createTempOrder(@RequestBody TempOrderRequestDto dto) {
        // 상품 금액 계산 (단순 계산 예시)
        int totalAmount = dto.getPrice() * dto.getQuantity();

        // OrderService로 주문 생성
        Order order = orderService.createOrderForPayment(dto.getProductName(), totalAmount);

        // 프론트에서 Toss 결제창 실행에 필요한 정보 반환
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("orderName", dto.getProductName());
        response.put("amount", totalAmount);
        return response;
    }
}