package com.app84soft.check_in.intergration;

import java.time.LocalDateTime;
import java.util.List;

public interface ExternalNhanhClient {
    record Order(
            long id,                       // nhanh_order_id
            LocalDateTime createdAt,
            String customerPhone,
            String paymentChannel,
            Long codToCollect,
            String carrier,                // "Giaohangnhanh", ...
            String carrierCode,            // mapping sang GHN
            String status,
            List<Item> items
    ) {}
    record Item(String sku, String size, Long unitPrice, Integer quantity) {}

    record Page<T>(List<T> items, boolean last) {}

    Page<Order> listOrders(LocalDateTime from, LocalDateTime to, int page, int size);
    Order getOrderById(long id);
}

