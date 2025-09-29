package com.app84soft.check_in.services.sheet;

import com.app84soft.check_in.other_service.ghn.GhnSheetService;
import com.app84soft.check_in.other_service.nhanh.NhanhClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Lấy dữ liệu từ POS Nhanh (index) rồi lưu vào DB cục bộ.
 * - bootstrapLatest: lấy page=1, limit=N mới nhất
 * - triggerFullSyncAsync: full-sync theo cửa sổ ngày gần đây
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BootstrapService {

    private final NhanhClient nhanhClient;
    private final GhnSheetService ghnSheet;
    private final SheetStore store; // JdbcTemplate-based

    private final ObjectMapper om = new ObjectMapper();

    /** Lấy page=1, limit=N từ Nhanh (không filter ngày) → upsert DB, enrich GHN nếu cần */
    public int bootstrapLatest(int limit) {
        try {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("page", "1");
            q.put("limit", String.valueOf(limit));

            Map<String, Object> resp = nhanhClient.listOrdersIndex(q);
            Map<String, Object> data = asMap(resp.get("data"));

            List<Map<String, Object>> orders = extractOrders(data);
            int saved = 0;

            for (Map<String, Object> o : orders) {
                Long nhanhId = asLong(o.get("id"));
                if (nhanhId == null) continue;

                // upsert nhanh_orders + nhanh_order_items
                store.upsertNhanhOrder(o);
                store.upsertNhanhItems(o); // giữ 1 tham số Map

                // enrich GHN nếu là GHN
                String carrier = s(o.get("carrierName"));
                String code    = s(o.get("carrierCode"));
                boolean isGHN  = (code != null && code.startsWith("NVS"))
                        || normalize(carrier).contains("ghn")
                        || normalize(carrier).contains("giaohangnhanh");

                if (isGHN && code != null) {
                    var w = ghnSheet.one(code);
                    if (w != null) store.upsertGhnOrderFromWhiteRow(w);
                }
                saved++;
            }
            return saved;
        } catch (Exception e) {
            log.error("bootstrapLatest failed", e);
            return 0;
        }
    }

    @Async
    public void refreshLatestAsync(int limit) {
        bootstrapLatest(limit);
    }

    /** Full sync nền: quét theo cửa sổ 7 ngày gần đây + paging */
    @Async
    public void triggerFullSyncAsync() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(7);
        int page = 1;
        int per  = 100;

        while (true) {
            try {
                Map<String, String> q = new LinkedHashMap<>();
                q.put("fromDate", from.toString());
                q.put("toDate",   to.plusDays(1).toString()); // end-exclusive
                q.put("page", String.valueOf(page));
                q.put("limit", String.valueOf(per));

                Map<String, Object> resp = nhanhClient.listOrdersIndex(q);
                Map<String, Object> data = asMap(resp.get("data"));
                List<Map<String, Object>> chunk = extractOrders(data);
                if (chunk.isEmpty()) break;

                for (Map<String, Object> o : chunk) {
                    Long nhanhId = asLong(o.get("id"));
                    if (nhanhId == null) continue;

                    store.upsertNhanhOrder(o);
                    store.upsertNhanhItems(o);

                    String carrier = s(o.get("carrierName"));
                    String code    = s(o.get("carrierCode"));
                    boolean isGHN  = (code != null && code.startsWith("NVS"))
                            || normalize(carrier).contains("ghn")
                            || normalize(carrier).contains("giaohangnhanh");

                    if (isGHN && code != null) {
                        var w = ghnSheet.one(code);
                        if (w != null) store.upsertGhnOrderFromWhiteRow(w);
                    }
                }
                if (chunk.size() < per) break;
                page++;
            } catch (Exception e) {
                log.warn("full sync page {} failed: {}", page, e.getMessage());
                break;
            }
        }
    }

    /* ================= helpers ================= */

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractOrders(Map<String, Object> data) {
        Object ordersObj = data.get("orders") != null ? data.get("orders") : data.get("items");
        List<Map<String, Object>> out = new ArrayList<>();
        if (ordersObj instanceof Map<?, ?> m) {
            m.values().forEach(v -> out.add(asMap(v)));
        } else if (ordersObj instanceof List<?> l) {
            for (Object v : l) if (v instanceof Map) out.add(asMap(v));
        }
        return out;
    }

    private static Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> r = new LinkedHashMap<>();
            m.forEach((k, v) -> r.put(String.valueOf(k), v));
            return r;
        }
        return new LinkedHashMap<>();
    }

    private static String s(Object o) { return o == null ? null : String.valueOf(o); }

    private static Long asLong(Object o) {
        try { return o == null ? null : Long.valueOf(String.valueOf(o)); }
        catch (Exception e) { return null; }
    }

    private static String normalize(String x) {
        if (x == null) return "";
        String n = java.text.Normalizer.normalize(x, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "");
        return n;
    }
}
