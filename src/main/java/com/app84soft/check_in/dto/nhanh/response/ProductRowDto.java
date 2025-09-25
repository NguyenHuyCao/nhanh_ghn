package com.app84soft.check_in.dto.nhanh.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRowDto {
    private String orderId;
    private String orderDate;
    private String saleChannel;
    private String depot;
    private String sku;
    private String productName;
    private String barcode;
    private String image;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal lineDiscount;
    private BigDecimal lineAmount;
    private BigDecimal avgCost;
}