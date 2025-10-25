package com.ll.P_A.payment.order;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pay/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
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

    // 주문 단건 조회
    @GetMapping("/{orderId}")
    public OrderResponseDto get(@PathVariable Long orderId) {
        var order = orderService.get(orderId);
        return OrderResponseDto.from(order);
    }
}