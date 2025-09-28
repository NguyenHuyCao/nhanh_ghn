package com.app84soft.check_in.intergration;


import java.time.LocalDateTime;

public interface ExternalGhnClient {
    record GhnOrder(
            String orderCode,
            String clientOrderCode,
            String shipStatus,
            LocalDateTime deliveredAt,
            Long shipFee,
            Long codAmount,
            String returnNote
    ) {}

    GhnOrder getOrder(String orderCode);
}

