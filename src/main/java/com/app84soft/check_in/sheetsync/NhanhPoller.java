package com.app84soft.check_in.sheetsync;

import com.app84soft.check_in.other_service.nhanh.NhanhClient;
import com.app84soft.check_in.repositories.shipping.SheetSyncRepos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Poller POS Nhanh:
 * - Lấy theo watermark (ms epoch) và cắt thành các frame theo phút (config)
 * - toDate gửi end-exclusive => luôn +1 ngày
 * - Khởi tạo watermark mặc định (nếu chưa có) là now-3day để tránh 1970
 * - Cập nhật watermark sau mỗi batch
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NhanhPoller {

    private final SheetSyncConfig cfg;
    private final NhanhClient nhanhClient;
    private final SheetSyncRepos repo;

    // 2000-01-01 00:00:00 UTC (để chống 1970)
    private static final long MIN_VALID_MS = 946684800000L;

    @Scheduled(fixedDelayString = "${sheetsync.poll.nhanh-fixed-delay-ms:30000}")
    public void run() {
        if (!cfg.isEnabled()) return;

        try {
            long wm  = repo.getNhanhWatermark();              // epoch ms; =0 nếu chưa có
            long now = System.currentTimeMillis();

            // Nếu watermark rỗng/không hợp lệ -> lùi 3 ngày
            long defaultSince = now - 3L * 24 * 60 * 60 * 1000;
            long sinceMs = (wm <= MIN_VALID_MS) ? defaultSince : wm;

            // Đệm lùi 60s để tránh miss, nhưng không nhỏ hơn mốc an toàn
            sinceMs = Math.max(MIN_VALID_MS, sinceMs - 60_000);

            long untilMs = now;
            long stepMs  = cfg.getWindow().getNhanhMinutes() * 60_000L;
            if (stepMs <= 0) stepMs = 20 * 60_000L; // fallback 20'

            log.info("NhanhPoller start since={} until={} step={}min",
                    tsToDateTime(sinceMs), tsToDateTime(untilMs), stepMs/60000);

            for (long frameStart = sinceMs; frameStart < untilMs; frameStart += stepMs) {
                long frameEnd = Math.min(frameStart + stepMs, untilMs);

                // POS end-exclusive -> toDate phải +1 ngày
                String fromDate = tsToDate(frameStart);
                String toDate   = tsToDate(frameEnd + 24L * 60 * 60 * 1000);

                int page = 1;
                while (true) {
                    Map<String, String> q = new LinkedHashMap<>();
                    q.put("fromDate", fromDate);
                    q.put("toDate",   toDate);
                    q.put("page",     String.valueOf(page));
                    q.put("limit",    "200");

                    Map<String, Object> res  = nhanhClient.listOrdersIndex(q);
                    Map<String, Object> data = castMap(res.get("data"));

                    List<Map<String, Object>> orders = readOrders(data);
                    if (orders.isEmpty()) break;

                    long maxUpd = wm;
                    for (var o : orders) {
                        repo.upsertNhanhOrder(o);
                        var items = readItems(o);
                        repo.upsertNhanhItems(Long.parseLong(String.valueOf(o.get("id"))), items);
                        // merge sheet_rows ngay (nếu bạn có dùng bảng sheet_rows)
                        items.forEach(it -> repo.upsertSheetRow(o, it, null));

                        Object uo = o.get("updatedAt");
                        if (uo != null) {
                            try {
                                long u = Long.parseLong(String.valueOf(uo));
                                if (u > maxUpd) maxUpd = u;
                            } catch (Exception ignore) {}
                        }
                    }

                    // cập nhật WM
                    if (maxUpd > wm) {
                        repo.saveNhanhWatermark(maxUpd);
                        wm = maxUpd;
                    }

                    Integer totalPages = asInt(data.get("totalPages"));
                    if (totalPages == null || page >= totalPages) break;
                    page++;
                }
            }

        } catch (Exception e) {
            log.warn("Nhanh poller error: {}", e.toString());
        }
    }

    /* ================= helpers ================= */

    private static Map<String, Object> castMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> r = new LinkedHashMap<>();
            m.forEach((k, v) -> r.put(String.valueOf(k), v));
            return r;
        }
        return Map.of();
    }

    private static List<Map<String, Object>> readOrders(Map<String, Object> data) {
        Object ordersObj = data.getOrDefault("orders", data.get("items"));
        if (!(ordersObj instanceof Map || ordersObj instanceof List)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        if (ordersObj instanceof Map<?, ?> m) {
            m.values().forEach(v -> out.add(castMap(v)));
        } else {
            for (Object v : (List<?>) ordersObj) out.add(castMap(v));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> readItems(Map<String, Object> o) {
        Object items = Optional.ofNullable(o.get("products"))
                .orElse(Optional.ofNullable(o.get("items")).orElse(o.get("orderItems")));
        if (!(items instanceof List<?> l)) return List.of(Map.of("dummy", true)); // vẫn tạo 1 dòng theo đơn
        List<Map<String, Object>> r = new ArrayList<>();
        for (Object e : l) if (e instanceof Map<?, ?> m) r.add(castMap(m));
        return r.isEmpty() ? List.of(Map.of("dummy", true)) : r;
    }

    private static Integer asInt(Object o) {
        try { return o == null ? null : Integer.valueOf(String.valueOf(o)); }
        catch (Exception e) { return null; }
    }

    private static String tsToDate(long ms) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(ms));
    }
    private static String tsToDateTime(long ms) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(ms));
    }
}
