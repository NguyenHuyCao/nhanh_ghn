package com.app84soft.check_in.dto.nhanh.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShippingRowDto {
    private String orderId;
    private String statusName;
    private String statusCode;
    private String shippingStatus;
    private String carrierName;
    private String trackingCode;
    private BigDecimal shipFee;
    private BigDecimal codFee;
    private BigDecimal returnFee;
    private BigDecimal overWeightShipFee;
    private BigDecimal declaredFee;
    private String sendCarrierDate;
    private String deliveryDate;
    private String packedAt;
}
