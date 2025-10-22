package com.ll.P_A.payment.order;

public record OrderRequestDto(
        Long buyerId,
        Long listingId,
        Long amount,
        ShippingInfoDto shippingInfo
) { }

