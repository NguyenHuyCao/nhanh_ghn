package com.app84soft.check_in.repositories.shipping;

import com.app84soft.check_in.services.sheet.SheetStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai tối giản: - Watermark đọc/ghi bảng sync_state - Upsert Nhanh delegating xuống
 * SheetStore (đã có sẵn) - Upsert SheetRow: no-op để không phá API hiện có - Upsert GHN: mapping cơ
 * bản từ Map
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SheetSyncReposImpl implements SheetSyncRepos {

  private final JdbcTemplate jdbc;
  private final SheetStore store;

  /* ================= Watermark ================= */

  @Override
  public long getNhanhWatermark() {
    Long wm = jdbc.query("SELECT last_id FROM sync_state WHERE source='nhanh' LIMIT 1",
        (rs, i) -> (Long) rs.getObject("last_id")).stream().findFirst().orElse(0L);

    jdbc.update("INSERT IGNORE INTO sync_state(source) VALUES ('nhanh')");
    return wm == null ? 0L : wm;
  }

  @Override
  public void saveNhanhWatermark(long epochMs) {
    jdbc.update("""
        UPDATE sync_state
           SET last_id=?, updated_ts=NOW()
         WHERE source='nhanh'
        """, epochMs);
  }

  @Override
  public LocalDateTime getGhnWatermark() {
    LocalDateTime t = jdbc
        .query("SELECT last_created_at FROM sync_state WHERE source='ghn' LIMIT 1",
            (rs, i) -> rs.getTimestamp("last_created_at") == null ? null
                : rs.getTimestamp("last_created_at").toLocalDateTime())
        .stream().findFirst().orElse(null);

    jdbc.update("INSERT IGNORE INTO sync_state(source) VALUES ('ghn')");
    return t;
  }

  @Override
  public void saveGhnWatermark(LocalDateTime t) {
    jdbc.update("""
        UPDATE sync_state
           SET last_created_at=?, updated_ts=NOW()
         WHERE source='ghn'
        """, t == null ? null : Timestamp.valueOf(t));
  }

  /* ================= Nhanh ================= */

  @Override
  public void upsertNhanhOrder(Map<String, Object> order) {
    // Ủy quyền cho SheetStore
    store.upsertNhanhOrder(order);
  }

  @Override
  public void upsertNhanhItems(long orderId, List<Map<String, Object>> items) {
    // SheetStore chỉ nhận Map order -> dựng map có id + products rồi ủy quyền
    Map<String, Object> order = new LinkedHashMap<>();
    order.put("id", orderId);
    order.put("products", items == null ? List.of() : items);
    store.upsertNhanhItems(order);
  }

  @Override
  public void upsertSheetRow(Map<String, Object> nhanhOrder, Map<String, Object> nhanhItem,
      Map<String, Object> ghnOrNull) {
    // NO-OP an toàn (bạn đang join trực tiếp từ bảng nhanh_* + ghn_orders)
    // Nếu cần ghi sheet_rows, bật block SQL trước đó.
  }

  /* ================= GHN ================= */

  @Override
  public void upsertGhnOrder(Map<String, Object> m) {
    String orderCode = s(m.get("order_code"));
    if (orderCode != null && orderCode.endsWith("_PR")) {
      log.debug("Skip GHN _PR {}", orderCode);
      return;
    }
    String clientOrderCode = s(first(m.get("client_order_code"), m.get("client_code")));
    LocalDateTime deliveredAt = toLdt(first(m.get("delivered_at"), m.get("finish_time")));
    Long shipFee = asLong(first(m.get("fee"), m.get("ship_fee")));
    Long codAmount = asLong(first(m.get("cod_amount"), m.get("cod")));
    String shipStatus = s(first(m.get("status"), m.get("ship_status")));
    String returnNote = s(m.get("return_note"));

    jdbc.update(
        """
            INSERT INTO ghn_orders
              (order_code, is_pr, client_order_code, delivered_at, ship_fee, cod_amount, ship_status, return_note,
               bank_collected_at, bank_amount)
            VALUES (?, 0, ?, ?, ?, ?, ?, ?, NULL, NULL)
            ON DUPLICATE KEY UPDATE
              client_order_code = VALUES(client_order_code),
              delivered_at      = VALUES(delivered_at),
              ship_fee          = VALUES(ship_fee),
              cod_amount        = VALUES(cod_amount),
              ship_status       = VALUES(ship_status),
              return_note       = VALUES(return_note)
            """,
        orderCode, clientOrderCode, deliveredAt == null ? null : Timestamp.valueOf(deliveredAt),
        shipFee, codAmount, shipStatus, returnNote);
  }

  /* ================= helpers ================= */

  private static Object first(Object... arr) {
    for (Object x : arr)
      if (x != null)
        return x;
    return null;
  }

  private static String s(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private static Long asLong(Object o) {
    try {
      return o == null ? null : Long.valueOf(String.valueOf(o));
    } catch (Exception e) {
      return null;
    }
  }

  private static LocalDateTime toLdt(Object v) {
    if (v == null)
      return null;
    try {
      String s = String.valueOf(v);
      if (s.matches("\\d+")) {
        long n = Long.parseLong(s);
        if (s.length() <= 10)
          n *= 1000L;
        return new Timestamp(n).toLocalDateTime();
      }
      return Timestamp.valueOf(s.replace('T', ' ')).toLocalDateTime();
    } catch (Exception ignore) {
      return null;
    }
  }
}
