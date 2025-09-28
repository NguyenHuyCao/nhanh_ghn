package com.app84soft.check_in.sheetsync;

import com.app84soft.check_in.other_service.nhanh.NhanhClient;
import com.app84soft.check_in.repositories.shipping.SheetSyncRepos;
import com.app84soft.check_in.repositories.shipping.SheetSyncReposImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class NhanhPoller {

    private final SheetSyncConfig cfg;
    private final NhanhClient nhanhClient;
    private final SheetSyncRepos repo;

    @Scheduled(fixedDelayString = "${sheetsync.poll.nhanh-fixed-delay-ms:30000}")
    public void run() {
        if (!cfg.isEnabled())
            return;
        try {
            long wm = repo.getNhanhWatermark(); // epoch
            long now = Instant.now().toEpochMilli();
            long from = Math.max(0, wm - 60_000); // đệm 60s
            long to = now + 86_400_000; // end-exclusive (POS đặc thù)

            // dựng tham số data cho /api/order/index
            // chia frame theo cửa sổ phút để tránh response to
            long step = cfg.getWindow().getNhanhMinutes() * 60_000L;
            for (long f = from; f < to; f += step) {
                long frameTo = Math.min(f + step, to);
                int page = 1;
                while (true) {
                    Map<String, String> q = new LinkedHashMap<>();
                    q.put("fromDate", tsToDate(f));
                    q.put("toDate", tsToDate(frameTo)); // đã +1d ở client của bạn, nên giữ nguyên
                    q.put("page", String.valueOf(page));
                    q.put("limit", "200");

                    Map<String, Object> res = nhanhClient.listOrdersIndex(q);
                    Map<String, Object> data = castMap(res.get("data"));

                    List<Map<String, Object>> orders = readOrders(data);
                    if (orders.isEmpty())
                        break;

                    long maxUpd = wm;
                    for (var o : orders) {
                        repo.upsertNhanhOrder(o);
                        var items = readItems(o);
                        repo.upsertNhanhItems(Long.parseLong(String.valueOf(o.get("id"))), items);
                        // enqueue merge ngay (ở đây merge inline cho gọn)
                        items.forEach(it -> repo.upsertSheetRow(o, it, null));
                        Object uo = o.get("updatedAt");
                        if (uo != null)
                            maxUpd = Math.max(maxUpd, Long.parseLong(String.valueOf(uo)));
                    }
                    if (maxUpd > wm) {
                        repo.saveNhanhWatermark(maxUpd);
                        wm = maxUpd;
                    }

                    Integer totalPages = asInt(data.get("totalPages"));
                    if (totalPages == null || page >= totalPages)
                        break;
                    page++;
                }
            }

        } catch (Exception e) {
            log.warn("Nhanh poller error: {}", e.toString());
        }
    }

    /* helpers */
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
        if (!(ordersObj instanceof Map || ordersObj instanceof List))
            return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        if (ordersObj instanceof Map<?, ?> m) {
            m.values().forEach(v -> out.add(castMap(v)));
        } else {
            for (Object v : (List<?>) ordersObj)
                out.add(castMap(v));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> readItems(Map<String, Object> o) {
        Object items = Optional.ofNullable(o.get("products"))
                .orElse(Optional.ofNullable(o.get("items")).orElse(o.get("orderItems")));
        if (!(items instanceof List<?> l))
            return List.of(Map.of("dummy", true)); // vẫn tạo 1 dòng theo đơn
        List<Map<String, Object>> r = new ArrayList<>();
        for (Object e : l)
            if (e instanceof Map<?, ?> m)
                r.add(castMap(m));
        return r.isEmpty() ? List.of(Map.of("dummy", true)) : r;
    }

    private static Integer asInt(Object o) {
        try {
            return o == null ? null : Integer.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private static String tsToDate(long ms) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(ms));
    }
}
