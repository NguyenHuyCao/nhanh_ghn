package com.app84soft.check_in.sheetsync;

import com.app84soft.check_in.other_service.ghn.GhnClient;
import com.app84soft.check_in.repositories.shipping.SheetSyncRepos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class GhnPoller {

    private final SheetSyncConfig cfg;
    private final GhnClient ghn;
    private final SheetSyncRepos repo;

    @Scheduled(fixedDelayString = "${sheetsync.poll.ghn-fixed-delay-ms:30000}")
    public void run() {
        if (!cfg.isEnabled())
            return;
        try {
            LocalDateTime wm = repo.getGhnWatermark();
            LocalDateTime from =
                    wm == null ? LocalDateTime.now().minusHours(cfg.getWindow().getGhnHours())
                            : wm.minusSeconds(120); // đệm
            LocalDateTime to = LocalDateTime.now();

            int page = 1;
            while (true) {
                Map<String, Object> body = new LinkedHashMap<>();
                Map<String, Object> filter = new LinkedHashMap<>();
                filter.put("from_date", from.toLocalDate().toString());
                filter.put("to_date", to.toLocalDate().toString());
                body.put("page", page);
                body.put("limit", 200);
                body.put("filter", filter);

                Map<String, Object> res = ghn.listOrders(body);
                Map<String, Object> data = castMap(res.get("data"));

                List<Map<String, Object>> orders =
                        listOfMap(first(data.get("orders"), data.get("items"), data.get("data")));
                if (orders.isEmpty())
                    break;

                LocalDateTime maxUpd = wm;
                for (var m : orders) {
                    repo.upsertGhnOrder(m);
                    // merge nhanh chóng vào sheet_rows nếu đã có bản nhanh
                    // (ở đây chúng ta chỉ upsert sheet với cột GHN, giữ nguyên các cột khác)
                    // — đơn giản nhất là đợi lần poll Nhanh tiếp theo sẽ upsert đủ;
                    // nếu muốn cập nhật ngay thì cần query nhanh_orders + items để điền.
                }
                // cập nhật watermark thô theo updated_date/finish_time đã được map trong repo
                maxUpd = LocalDateTime.now(); // safe default
                repo.saveGhnWatermark(maxUpd);

                Integer total = asInt(first(data.get("total"), data.get("total_orders")));
                Integer limit = asInt(first(data.get("limit"), 200));
                if (total == null || limit == null || page * limit >= total)
                    break;
                page++;
            }

        } catch (Exception e) {
            log.warn("GHN poller error: {}", e.toString());
        }
    }

    /* helpers */
    private static Object first(Object... arr) {
        for (Object x : arr)
            if (x != null)
                return x;
        return null;
    }

    private static Map<String, Object> castMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> r = new LinkedHashMap<>();
            m.forEach((k, v) -> r.put(String.valueOf(k), v));
            return r;
        }
        return Map.of();
    }

    private static List<Map<String, Object>> listOfMap(Object o) {
        if (!(o instanceof List<?> l))
            return List.of();
        List<Map<String, Object>> r = new ArrayList<>();
        for (Object e : l)
            if (e instanceof Map<?, ?> m)
                r.add(castMap(m));
        return r;
    }

    private static Integer asInt(Object o) {
        try {
            return o == null ? null : Integer.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
