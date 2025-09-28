package com.app84soft.check_in.services.sheet;

import com.app84soft.check_in.dto.ghn.response.WhiteRowDto;
import com.app84soft.check_in.intergration.ExternalGhnClient;
import com.app84soft.check_in.intergration.ExternalNhanhClient;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp thao tác DB (JdbcTemplate) cho sheet: nhanh_orders, nhanh_order_items, ghn_orders.
 * Có đủ 2 overload cho upsertNhanhItems:
 *   - nhận Map<String,Object> order (luồng NhanhClient.listOrdersIndex)
 *   - nhận ExternalNhanhClient.Order (luồng ScheduledSyncService)
 */
@Component
@RequiredArgsConstructor
public class SheetStore {
    private final JdbcTemplate jdbc;

    /* ---------- Nhanh: Upsert order ---------- */

    /** Upsert theo Map (order lấy từ NhanhClient.listOrdersIndex) */
    public void upsertNhanhOrder(Map<String, Object> o) {
        if (o == null) return;

        Long id = asLong(o.get("id"));
        LocalDateTime createdAt = toLdt(o.get("createdDateTime"), o.get("createdTime"), o.get("createdAt"));
        String phone = s(first(o.get("customerMobile"), o.get("customerPhone")));
        String paymentChannel = s(first(o.get("paymentMethod"), o.get("paymentMethodName")));
        if (isBlank(paymentChannel)) paymentChannel = "COD";

        Long codToCollect = asLong(first(o.get("calcTotalMoney"), o.get("grandTotal"), o.get("total")));
        String carrier     = s(first(o.get("carrierName"), o.get("shippingPartner")));
        String carrierCode = s(first(o.get("carrierCode"), o.get("shipmentCode")));
        String status      = s(first(o.get("statusName"), o.get("status")));

        jdbc.update("""
            INSERT INTO nhanh_orders
              (id, created_at, customer_phone, payment_channel, cod_to_collect, carrier, carrier_code, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              created_at      = VALUES(created_at),
              customer_phone  = VALUES(customer_phone),
              payment_channel = VALUES(payment_channel),
              cod_to_collect  = VALUES(cod_to_collect),
              carrier         = VALUES(carrier),
              carrier_code    = VALUES(carrier_code),
              status          = VALUES(status)
            """,
                id,
                createdAt == null ? null : Timestamp.valueOf(createdAt),
                phone,
                paymentChannel,
                codToCollect,
                carrier,
                carrierCode,
                status
        );
    }

    /** Upsert theo DTO Order (luồng ScheduledSyncService) */
    public void upsertNhanhOrder(ExternalNhanhClient.Order o) {
        if (o == null) return;
        String paymentChannel = o.paymentChannel();
        if (isBlank(paymentChannel)) paymentChannel = "COD";

        jdbc.update("""
            INSERT INTO nhanh_orders
              (id, created_at, customer_phone, payment_channel, cod_to_collect, carrier, carrier_code, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              created_at      = VALUES(created_at),
              customer_phone  = VALUES(customer_phone),
              payment_channel = VALUES(payment_channel),
              cod_to_collect  = VALUES(cod_to_collect),
              carrier         = VALUES(carrier),
              carrier_code    = VALUES(carrier_code),
              status          = VALUES(status)
            """,
                o.id(),
                o.createdAt() == null ? null : Timestamp.valueOf(o.createdAt()),
                o.customerPhone(),
                paymentChannel,
                o.codToCollect(),
                o.carrier(),
                o.carrierCode(),
                o.status()
        );
    }

    /* ---------- Nhanh: Upsert items (2 overload) ---------- */

    /** Overload 1: nhận Map order (từ NhanhClient.listOrdersIndex) */
    @SuppressWarnings("unchecked")
    public void upsertNhanhItems(Map<String, Object> order) {
        if (order == null) return;

        Long orderId = asLong(order.get("id"));
        if (orderId == null) return;

        Object rawItems = first(order.get("products"), order.get("items"), order.get("orderItems"));
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        if (rawItems instanceof List<?> l) {
            for (Object e : l) if (e instanceof Map<?, ?> m) items.add(castMap(m));
        }

        if (items.isEmpty()) {
            // vẫn tạo 1 dòng “đại diện” nếu POS không có item
            jdbc.update("""
                INSERT INTO nhanh_order_items (order_id, sku, `size`, unit_price)
                VALUES (?, ?, ?, ?)
                """, orderId, null, null, null);
            return;
        }

        for (Map<String, Object> it : items) {
            String sku  = s(first(it.get("productCode"), it.get("sku"), it.get("productId")));
            String size = s(first(it.get("size"), it.get("variantName")));
            Long price  = asLong(first(it.get("price"), it.get("sellPrice"), it.get("unitPrice")));

            jdbc.update("""
                INSERT INTO nhanh_order_items (order_id, sku, `size`, unit_price)
                VALUES (?, ?, ?, ?)
                """, orderId, sku, size, price);
        }
    }

    /** Overload 2: nhận DTO Order (luồng ScheduledSyncService) */
    public void upsertNhanhItems(ExternalNhanhClient.Order o) {
        if (o == null) return;

        Long orderId = o.id();
        var items = o.items();

        if (items == null || items.isEmpty()) {
            jdbc.update("""
                INSERT INTO nhanh_order_items (order_id, sku, `size`, unit_price)
                VALUES (?, ?, ?, ?)
                """, orderId, null, null, null);
            return;
        }

        for (var it : items) {
            jdbc.update("""
                INSERT INTO nhanh_order_items (order_id, sku, `size`, unit_price)
                VALUES (?, ?, ?, ?)
                """, orderId, it.sku(), it.size(), it.unitPrice());
        }
    }

    /* ---------- GHN: Upsert ---------- */

    public void upsertGhnOrder(ExternalGhnClient.GhnOrder go) {
        if (go == null) return;
        jdbc.update("""
            INSERT INTO ghn_orders
              (order_code, client_order_code, delivered_at, ship_fee, cod_amount, ship_status, return_note,
               bank_collected_at, bank_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              client_order_code = VALUES(client_order_code),
              delivered_at      = VALUES(delivered_at),
              ship_fee          = VALUES(ship_fee),
              cod_amount        = VALUES(cod_amount),
              ship_status       = VALUES(ship_status),
              return_note       = VALUES(return_note),
              bank_collected_at = VALUES(bank_collected_at),
              bank_amount       = VALUES(bank_amount)
            """,
                go.orderCode(),
                go.clientOrderCode(),
                go.deliveredAt() == null ? null : Timestamp.valueOf(go.deliveredAt()),
                go.shipFee(),
                go.codAmount(),
                go.shipStatus(),
                go.returnNote(),
                null,
                null
        );
    }

    public void upsertGhnOrderFromWhiteRow(WhiteRowDto w) {
        if (w == null) return;
        jdbc.update("""
            INSERT INTO ghn_orders
              (order_code, client_order_code, delivered_at, ship_fee, cod_amount, ship_status, return_note,
               bank_collected_at, bank_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              client_order_code = VALUES(client_order_code),
              delivered_at      = VALUES(delivered_at),
              ship_fee          = VALUES(ship_fee),
              cod_amount        = VALUES(cod_amount),
              ship_status       = VALUES(ship_status),
              return_note       = VALUES(return_note),
              bank_collected_at = VALUES(bank_collected_at),
              bank_amount       = VALUES(bank_amount)
            """,
                w.getOrderCode(),
                w.getClientOrderCode(),
                w.getDeliveredAt() == null ? null : Timestamp.valueOf(w.getDeliveredAt()),
                w.getShipFee(),
                w.getCodAmount(),
                w.getShipStatus(),
                w.getReturnNote(),
                null,
                null
        );
    }

    /* ================= helpers ================= */

    private static Map<String, Object> castMap(Map<?, ?> m) {
        Map<String, Object> r = new LinkedHashMap<>();
        m.forEach((k, v) -> r.put(String.valueOf(k), v));
        return r;
    }

    private static Object first(Object... arr) { for (Object x : arr) if (x != null) return x; return null; }

    private static String s(Object o) { return o == null ? null : String.valueOf(o); }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static Long asLong(Object o) {
        try { return o == null ? null : Long.valueOf(String.valueOf(o)); }
        catch (Exception e) { return null; }
    }

    private static LocalDateTime toLdt(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) continue;
            String v = String.valueOf(c);
            try {
                long n = Long.parseLong(v);
                if (v.length() <= 10) n = n * 1000;
                return new Timestamp(n).toLocalDateTime();
            } catch (Exception ignore) {}
        }
        return null;
    }
}
