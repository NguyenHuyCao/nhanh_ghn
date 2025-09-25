package com.app84soft.check_in.dto.nhanh.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRowDto {
    private String orderId;
    private String orderDate;
    private String saleChannel;
    private BigDecimal subtotal;
    private BigDecimal orderDiscount;
    private BigDecimal shipFeeCustomer;
    private BigDecimal grandTotal;
    private BigDecimal deposit;
    private BigDecimal transfer;
    private BigDecimal credit;
    private BigDecimal points;
    private BigDecimal paid;
    private BigDecimal codToCollect;
    private String paymentMethod;
    private String paymentStatus;
}
