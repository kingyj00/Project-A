package com.ll.P_A.payment.order;

public record ShippingInfoDto(
        String receiverName,
        String phone,
        String address1,
        String address2,
        String zipcode
) {
    public ShippingInfo toEntity() {
        return ShippingInfo.builder()
                .receiverName(receiverName)
                .phone(phone)
                .address1(address1)
                .address2(address2)
                .zipcode(zipcode)
                .build();
    }

    public static ShippingInfoDto from(ShippingInfo si) {
        if (si == null) return null;
        return new ShippingInfoDto(
                si.getReceiverName(), si.getPhone(),
                si.getAddress1(), si.getAddress2(), si.getZipcode()
        );
    }
}