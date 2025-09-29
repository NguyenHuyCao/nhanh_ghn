package com.app84soft.check_in.services.sheet;

import com.app84soft.check_in.dto.ghn.response.WhiteRowDto;
import com.app84soft.check_in.intergration.ExternalGhnClient;
import com.app84soft.check_in.intergration.ExternalNhanhClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SheetStore {
    private final JdbcTemplate jdbc;

    /* ===================== Nhanh: ORDER ===================== */

    /** Upsert theo Map (luồng NhanhClient.listOrdersIndex) */
    public void upsertNhanhOrder(Map<String, Object> o) {
        if (o == null) return;
        Long id = asLong(o.get("id"));
        if (id == null) return;

        LocalDateTime createdAt = toLdt(o.get("createdDateTime"), o.get("createdTime"), o.get("createdAt"));
        if (createdAt == null) { // ADD: fallback updatedAt
            createdAt = toLdt(o.get("updatedAt")); // CHANGED
        }

        String phone = s(first(o.get("customerMobile"), o.get("customerPhone")));
        String paymentChannel = s(first(o.get("paymentMethod"), o.get("paymentMethodName")));
        if (isBlank(paymentChannel)) paymentChannel = "COD";
        Long codToCollect = asLong(first(o.get("calcTotalMoney"), o.get("grandTotal"), o.get("total")));
        String carrier     = s(first(o.get("carrierName"), o.get("shippingPartner")));
        String carrierCode = s(first(o.get("carrierCode"), o.get("shipmentCode")));
        String status      = s(first(o.get("statusName"), o.get("status")));

        int rows = jdbc.update("""
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
                phone, paymentChannel, codToCollect, carrier, carrierCode, status
        );
        log.debug("Store.upsertNhanhOrder id={} affected={}", id, rows);
    }

    /** Upsert theo DTO (luồng ScheduledSyncService) */
    public void upsertNhanhOrder(ExternalNhanhClient.Order o) {
        if (o == null) return;
        String paymentChannel = isBlank(o.paymentChannel()) ? "COD" : o.paymentChannel();

        int rows = jdbc.update("""
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
                o.customerPhone(), paymentChannel, o.codToCollect(),
                o.carrier(), o.carrierCode(), o.status()
        );
        log.debug("Store.upsertNhanhOrder(dto) id={} affected={}", o.id(), rows);
    }

    /* ===================== Nhanh: ITEMS (2 overload) ===================== */

    @SuppressWarnings("unchecked")
    public void upsertNhanhItems(Map<String, Object> order) {
        if (order == null) return;
        Long orderId = asLong(order.get("id"));
        if (orderId == null) return;

        Object rawItems = first(order.get("products"), order.get("items"), order.get("orderItems"));
        List<Map<String, Object>> items = new ArrayList<>();
        if (rawItems instanceof List<?> l) {
            for (Object e : l) if (e instanceof Map<?, ?> m) items.add(cast(m));
        }

        // Tổng số lượng toàn đơn (để phân bổ)
        int totalQty = 0;
        for (Map<String, Object> it : items) {
            Object q = first(it.get("quantity"), it.get("qty"), 1);
            int qi; try { qi = q == null ? 1 : Integer.parseInt(String.valueOf(q)); } catch (Exception ex) { qi = 1; }
            totalQty += Math.max(0, qi);
        }
        if (totalQty == 0) totalQty = 1; // tránh chia 0

        long deposit  = asLong(first(order.get("moneyDeposit")))  == null ? 0L : asLong(order.get("moneyDeposit"));
        long transfer = asLong(first(order.get("moneyTransfer"))) == null ? 0L : asLong(order.get("moneyTransfer"));

        final String SQL = """
            INSERT INTO nhanh_order_items (order_id, sku, `size`, unit_price, quantity, discount_total, deposit_alloc, transfer_alloc, revenue_item)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              `size`         = VALUES(`size`),
              unit_price     = VALUES(unit_price),
              quantity       = GREATEST(VALUES(quantity), quantity),
              discount_total = VALUES(discount_total),
              deposit_alloc  = VALUES(deposit_alloc),
              transfer_alloc = VALUES(transfer_alloc),
              revenue_item   = VALUES(revenue_item)
            """;

        int affected = 0;

        if (items.isEmpty()) {
            // vẫn tạo 1 dòng đại diện theo đơn (không có item)
            long allocDep  = deposit;
            long allocTran = transfer;
            long revenue   = 0L;
            affected += jdbc.update(SQL, orderId, "_NOITEM_" + orderId, null, null, 1, 0L, allocDep, allocTran, revenue);
            log.debug("Store.upsertNhanhItems orderId={} (no items) affected={}", orderId, affected);
            return;
        }

        for (Map<String, Object> it : items) {
            String sku = s(first(it.get("productCode"), it.get("sku"), it.get("productId")));
            if (isBlank(sku)) sku = "_NOITEM_" + orderId;

            String size = s(first(it.get("size"), it.get("variantName")));
            if (isBlank(size)) size = extractSizeFromSku(sku);

            Long unitPrice = asLong(first(it.get("price"), it.get("sellPrice"), it.get("unitPrice")));
            if (unitPrice == null) unitPrice = 0L;

            int qty; try { Object q = first(it.get("quantity"), it.get("qty"), 1); qty = q==null?1:Integer.parseInt(String.valueOf(q)); }
            catch (Exception ignore) { qty = 1; }

            long discountTotal = asLong(first(it.get("discount"), it.get("lineDiscount"))) == null
                    ? 0L : asLong(first(it.get("discount"), it.get("lineDiscount")));

            // phân bổ đặt cọc / chuyển khoản theo số lượng
            long allocDep  = Math.round((deposit  * 1.0 / totalQty) * qty);
            long allocTran = Math.round((transfer * 1.0 / totalQty) * qty);

            // doanh thu phát sinh theo mã (ngoài sàn): price*qty - discount_total
            long revenue = unitPrice * Math.max(0, qty) - Math.max(0, discountTotal);

            affected += jdbc.update(SQL, orderId, sku, size, unitPrice, qty, discountTotal, allocDep, allocTran, revenue);
        }
        log.debug("Store.upsertNhanhItems orderId={} items={} affected={}", orderId, items.size(), affected);
    }

    public void upsertNhanhItems(ExternalNhanhClient.Order o) {
        if (o == null) return;
        Long orderId = o.id();

        final String SQL = """
            INSERT INTO nhanh_order_items (order_id, sku, `size`, unit_price, quantity, discount_total, deposit_alloc, transfer_alloc, revenue_item)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              `size`         = VALUES(`size`),
              unit_price     = VALUES(unit_price),
              quantity       = GREATEST(VALUES(quantity), quantity),
              discount_total = VALUES(discount_total),
              deposit_alloc  = VALUES(deposit_alloc),
              transfer_alloc = VALUES(transfer_alloc),
              revenue_item   = VALUES(revenue_item)
            """;

        int affected = 0;
        if (o.items() == null || o.items().isEmpty()) {
            affected += jdbc.update(SQL, orderId, "_NOITEM_" + orderId, null, null, 1, 0L, 0L, 0L, 0L);
            log.debug("Store.upsertNhanhItems(dto) orderId={} (no items) affected={}", orderId, affected);
            return;
        }

        // Không có discount/deposit/transfer trong DTO đơn giản -> để 0
        for (var it : o.items()) {
            String sku = (it.sku() == null || it.sku().isBlank()) ? "_NOITEM_" + orderId : it.sku();
            String size = isBlank(it.size()) ? extractSizeFromSku(sku) : it.size();
            int qty = it.quantity() == null ? 1 : it.quantity();
            long unitPrice = it.unitPrice() == null ? 0L : it.unitPrice();
            affected += jdbc.update(SQL, orderId, sku, size, unitPrice, qty, 0L, 0L, 0L, unitPrice * Math.max(0, qty));
        }
        log.debug("Store.upsertNhanhItems(dto) orderId={} items={} affected={}", orderId, o.items().size(), affected);
    }

    /* ===================== GHN: ORDER ===================== */

    public void upsertGhnOrder(ExternalGhnClient.GhnOrder go) {
        if (go == null) return;
        int isPr = isPrCode(go.orderCode()) ? 1 : 0;

        int rows = jdbc.update("""
        INSERT INTO ghn_orders
          (order_code, is_pr, client_order_code, delivered_at, ship_fee, cod_amount, ship_status, return_note,
           bank_collected_at, bank_amount)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          is_pr             = VALUES(is_pr),
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
                isPr,
                go.clientOrderCode(),
                go.deliveredAt() == null ? null : Timestamp.valueOf(go.deliveredAt()),
                go.shipFee(),
                go.codAmount(),
                go.shipStatus(),
                go.returnNote(),
                null, null
        );
        log.debug("Store.upsertGhnOrder code={} affected={}", go.orderCode(), rows);
    }

    private static boolean isPrCode(String code) {
        return code != null && code.toUpperCase(Locale.ROOT).startsWith("PR");
    }


    public void upsertGhnOrderFromWhiteRow(WhiteRowDto w) {
        if (w == null) return;
        int isPr = isPrCode(w.getOrderCode()) ? 1 : 0;

        int rows = jdbc.update("""
        INSERT INTO ghn_orders
          (order_code, is_pr, client_order_code, delivered_at, ship_fee, cod_amount, ship_status, return_note,
           bank_collected_at, bank_amount)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          is_pr             = VALUES(is_pr),
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
                isPr,
                w.getClientOrderCode(),
                w.getDeliveredAt() == null ? null : Timestamp.valueOf(w.getDeliveredAt()),
                w.getShipFee(),
                w.getCodAmount(),
                w.getShipStatus(),
                w.getReturnNote(),
                null, null
        );
        log.debug("Store.upsertGhnOrder(white) code={} affected={}", w.getOrderCode(), rows);
    }


    /* ===================== helpers ===================== */
    private static Map<String, Object> cast(Map<?, ?> m) {
        Map<String, Object> r = new LinkedHashMap<>();
        m.forEach((k, v) -> r.put(String.valueOf(k), v));
        return r;
    }
    private static Object first(Object... arr) { for (Object x : arr) if (x != null) return x; return null; }
    private static String s(Object o) { return o == null ? null : String.valueOf(o); }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static Long asLong(Object o) { try { return o == null ? null : Long.valueOf(String.valueOf(o)); } catch (Exception e) { return null; } }
    private static LocalDateTime toLdt(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) continue;
            String v = String.valueOf(c).trim();
            if (v.isEmpty()) continue;
            try {
                if (v.matches("\\d+")) {
                    long n = Long.parseLong(v);
                    if (v.length() <= 10) n *= 1000L;
                    return new Timestamp(n).toLocalDateTime();
                }
                String norm = v.replace('T', ' ');
                return Timestamp.valueOf(norm).toLocalDateTime();
            } catch (Exception ignore) {}
        }
        return null;
    }
    private static String extractSizeFromSku(String sku) {
        if (isBlank(sku)) return null;
        int i = sku.lastIndexOf('-');
        if (i < 0 || i == sku.length() - 1) return null;
        String cand = sku.substring(i + 1).trim();
        String up = cand.toUpperCase(Locale.ROOT);
        List<String> wl = List.of("XS","S","M","L","XL","XXL","2XL","3XL","4XL","F","FREE","FREESIZE");
        return wl.contains(up) ? up : cand;
    }
}
