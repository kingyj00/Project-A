package com.ll.P_A.payment.order;

public record OrderRequestDto(
        Long buyerId,
        Long listingId,
        Long amount,
        ShippingInfoDto shippingInfo,
        String productName,
        Integer quantity
) {
    public boolean isTempOrder() {
        return (buyerId == null && shippingInfo == null);
    }
}