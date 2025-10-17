package com.ll.P_A.payment.order;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Embeddable
public class ShippingInfo {
    private String receiverName;
    private String phone;
    private String address1;
    private String address2;
    private String zipcode;
}