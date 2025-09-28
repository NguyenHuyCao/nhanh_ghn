package com.app84soft.check_in.services.sheet;

import com.app84soft.check_in.intergration.ExternalGhnClient;
import com.app84soft.check_in.intergration.ExternalNhanhClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledSyncService {

    private final JdbcTemplate jdbc;
    private final ExternalNhanhClient nhanh; // client chuẩn hóa cho Nhanh
    private final ExternalGhnClient ghn; // client chuẩn hóa cho GHN
    private final SheetStore store; // lưu DB

    /* ===================== Watermark helpers ===================== */

    private Mark getMark(String source) {
        var list =
                jdbc.query("SELECT source, last_created_at, last_id FROM sync_state WHERE source=?",
                        (rs, i) -> new Mark(rs.getString("source"),
                                rs.getTimestamp("last_created_at") == null ? null
                                        : rs.getTimestamp("last_created_at").toLocalDateTime(),
                                (Long) rs.getObject("last_id")),
                        source);
        if (list.isEmpty()) {
            jdbc.update("INSERT IGNORE INTO sync_state(source) VALUES (?)", source);
            return new Mark(source, null, null);
        }
        return list.get(0);
    }

    private void saveMark(String source, LocalDateTime t, Long id) {
        jdbc.update(
                "UPDATE sync_state SET last_created_at=?, last_id=?, updated_ts=NOW() WHERE source=?",
                t, id, source);
    }

    record Mark(String source, LocalDateTime t, Long id) {
    }

    /* ===================== 1) Backfill thủ công ===================== */

    @Transactional
    public long backfillNhanh(LocalDateTime from, LocalDateTime to) {
        long saved = 0;
        int page = 1, size = 200;

        while (true) {
            var p = nhanh.listOrders(from, to, page, size);
            if (p.items().isEmpty())
                break;

            for (var o : p.items()) {
                store.upsertNhanhOrder(o);
                store.upsertNhanhItems(o);

                if ("Giaohangnhanh".equalsIgnoreCase(o.carrier()) && o.carrierCode() != null) {
                    var go = ghn.getOrder(o.carrierCode());
                    if (go != null)
                        store.upsertGhnOrder(go);
                }
                saved++;
            }
            if (p.last())
                break;
            page++;
        }
        return saved;
    }

    /* ===================== 2) Incremental tự chạy ===================== */

    @Scheduled(fixedDelay = 30_000L, initialDelay = 10_000L) // mỗi 30s, chạy sau 10s khi start
    @Transactional
    public void incrementalNhanh() {
        try {
            var mk = getMark("nhanh");
            var since = mk.t() == null ? LocalDateTime.now().minusDays(3) : mk.t();

            int page = 1, size = 200;
            LocalDateTime maxT = mk.t();
            Long maxId = mk.id();

            while (true) {
                var p = nhanh.listOrders(since, null, page, size);
                if (p.items().isEmpty())
                    break;

                for (var o : p.items()) {
                    // bỏ qua nếu <= watermark
                    if (mk.t() != null) {
                        if (o.createdAt().isBefore(mk.t()))
                            continue;
                        if (o.createdAt().isEqual(mk.t()) && mk.id() != null && o.id() <= mk.id())
                            continue;
                    }

                    store.upsertNhanhOrder(o);
                    store.upsertNhanhItems(o);

                    if ("Giaohangnhanh".equalsIgnoreCase(o.carrier()) && o.carrierCode() != null) {
                        var go = ghn.getOrder(o.carrierCode());
                        if (go != null)
                            store.upsertGhnOrder(go);
                    }

                    // cập nhật watermark tạm
                    if (maxT == null || o.createdAt().isAfter(maxT)) {
                        maxT = o.createdAt();
                        maxId = o.id();
                    } else if (o.createdAt().isEqual(maxT)
                            && o.id() > (maxId == null ? -1 : maxId)) {
                        maxId = o.id();
                    }
                }

                if (p.last())
                    break;
                page++;
            }

            if (maxT != null)
                saveMark("nhanh", maxT, maxId);

        } catch (Exception e) {
            log.error("incrementalNhanh failed", e);
        }
    }

    /* ===================== 3) Refresh trạng thái GHN ===================== */

    @Scheduled(fixedDelay = 300_000L, initialDelay = 20_000L) // mỗi 5 phút, chờ 20s sau khi start
    @Transactional
    public void refreshGHN() {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    """
                                SELECT o.carrier_code
                                FROM nhanh_orders o
                                LEFT JOIN ghn_orders g ON g.order_code = o.carrier_code
                                WHERE o.carrier = 'Giaohangnhanh'
                                  AND o.created_at >= NOW() - INTERVAL 30 DAY
                                  AND o.carrier_code IS NOT NULL
                                  AND (g.order_code IS NULL OR g.ship_status IS NULL OR g.delivered_at IS NULL)
                                LIMIT 500
                            """);

            for (var r : rows) {
                var code = (String) r.get("carrier_code");
                var go = ghn.getOrder(code);
                if (go != null)
                    store.upsertGhnOrder(go);
            }
        } catch (Exception e) {
            log.error("refreshGHN failed", e);
        }
    }
}
