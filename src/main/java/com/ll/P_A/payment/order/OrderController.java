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

    // 일반 주문 생성 (배송 포함)
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

    // 주문 조회
    @GetMapping("/{orderId}")
    public OrderResponseDto get(@PathVariable Long orderId) {
        var order = orderService.get(orderId);
        return OrderResponseDto.from(order);
    }

    // 프론트 결제용 임시 주문 생성
    @PostMapping("/create")
    public Map<String, Object> createTempOrder(@RequestBody OrderRequestDto dto) {
        int quantity = dto.quantity() != null ? dto.quantity() : 1;
        int totalAmount = dto.amount().intValue() * quantity;

        Order order = orderService.createOrderForPayment(dto.productName(), totalAmount);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("orderName", dto.productName());
        response.put("amount", totalAmount);
        return response;
    }
}