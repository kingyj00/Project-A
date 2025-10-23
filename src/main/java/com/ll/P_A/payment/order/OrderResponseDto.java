package com.ll.P_A.payment.order;

public record OrderResponseDto(
        Long id,
        Long buyerId,
        Long listingId,
        Long amount,
        OrderStatus status,
        ShippingInfoDto shippingInfo
) {
    public static OrderResponseDto from(Order o) {
        return new OrderResponseDto(
                o.getId(), o.getBuyerId(), o.getListingId(), o.getAmount(),
                o.getStatus(), ShippingInfoDto.from(o.getShippingInfo())
        );
    }
}
