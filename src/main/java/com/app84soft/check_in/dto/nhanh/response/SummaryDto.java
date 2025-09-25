package com.app84soft.check_in.dto.nhanh.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummaryDto {
    private int count;
    private BigDecimal subtotal;
    private BigDecimal shipFeeCustomer;
    private BigDecimal grandTotal;
    private BigDecimal paid;
    private BigDecimal codToCollect;
    private Map<String, Group> byShippingStatus;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class Group {
        private int count;
        private BigDecimal
                grandTotal;
        private BigDecimal codToCollect;
    }
}
