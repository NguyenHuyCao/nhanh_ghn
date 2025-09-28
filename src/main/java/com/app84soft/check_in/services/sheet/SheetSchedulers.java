package com.app84soft.check_in.services.sheet;

import com.app84soft.check_in.other_service.ghn.GhnSheetService;
import com.app84soft.check_in.other_service.nhanh.NhanhClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Poll dữ liệu mới từ Nhanh/GHN rồi upsert vào DB local.
 */
@Component
@ConditionalOnProperty(value = "sheetsync.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class SheetSchedulers {

    private final NhanhClient nhanhClient;
    private final GhnSheetService ghnSheet;
    private final SheetStore store;

    /** Nhanh: quét cửa sổ hôm nay (end-exclusive) */
    @Scheduled(fixedDelayString = "${sheetsync.poll.nhanh-fixed-delay-ms:30000}")
    public void pollNhanh() {
        try {
            LocalDate now = LocalDate.now();
            Map<String, String> q = new LinkedHashMap<>();
            q.put("fromDate", now.toString());
            q.put("toDate",   now.plusDays(1).toString());
            q.put("page",     "1");
            q.put("limit",    "100");

            Map<String, Object> resp = nhanhClient.listOrdersIndex(q);
            Map<String, Object> data = asMap(resp.get("data"));
            List<Map<String, Object>> orders = extractOrders(data);

            for (Map<String, Object> o : orders) {
                Long id = asLong(o.get("id"));
                if (id == null) continue;

                // upsert nhanh_orders + items (theo overload nhận Map order)
                store.upsertNhanhOrder(o);
                store.upsertNhanhItems(o);

                // enrich GHN nếu có
                String carrier = s(o.get("carrierName"));
                String code    = s(o.get("carrierCode"));
                boolean isGHN  = (code != null && code.startsWith("NVS"))
                        || norm(carrier).contains("ghn")
                        || norm(carrier).contains("giaohangnhanh");
                if (isGHN && code != null) {
                    var w = ghnSheet.one(code);
                    if (w != null) store.upsertGhnOrderFromWhiteRow(w);
                }
            }
        } catch (Exception e) {
            log.warn("pollNhanh failed: {}", e.getMessage());
        }
    }

    /** GHN: kéo rộng 48h gần nhất để đảm bảo bắt đủ */
    @Scheduled(fixedDelayString = "${sheetsync.poll.ghn-fixed-delay-ms:30000}")
    public void pollGhn() {
        try {
            var page = ghnSheet.white(LocalDate.now().minusDays(2), LocalDate.now(), 1, 200);
            page.items.forEach(store::upsertGhnOrderFromWhiteRow);
        } catch (Exception e) {
            log.warn("pollGhn failed: {}", e.getMessage());
        }
    }

    /* ================= helpers ================= */

    private static Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> r = new LinkedHashMap<>();
            m.forEach((k, v) -> r.put(String.valueOf(k), v));
            return r;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractOrders(Map<String, Object> data) {
        Object ordersObj = data.get("orders") != null ? data.get("orders") : data.get("items");
        if (!(ordersObj instanceof List<?> l)) return List.of();
        List<Map<String, Object>> r = new ArrayList<>();
        for (Object e : l) if (e instanceof Map<?, ?> m) r.add(asMap(m));
        return r;
    }

    private static String s(Object o) { return o == null ? null : String.valueOf(o); }

    private static Long asLong(Object o) {
        try { return o == null ? null : Long.valueOf(String.valueOf(o)); }
        catch (Exception e) { return null; }
    }

    private static String norm(String x) {
        if (x == null) return "";
        return java.text.Normalizer.normalize(x, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "");
    }
}
