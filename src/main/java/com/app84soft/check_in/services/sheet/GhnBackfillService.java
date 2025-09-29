package com.app84soft.check_in.services.sheet;

import com.app84soft.check_in.other_service.ghn.GhnSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnBackfillService {
    private final JdbcTemplate jdbc;
    private final GhnSheetService ghn;
    private final SheetStore store;

    /** Backfill n mã GHN chưa có trong ghn_orders */
    public int backfillMissing(int batch) {
        List<String> codes = jdbc.queryForList("""
            SELECT o.carrier_code
            FROM nhanh_orders o
            LEFT JOIN ghn_orders go ON go.order_code = o.carrier_code
            WHERE o.carrier_code LIKE 'NVS%%' AND go.order_code IS NULL
            ORDER BY o.created_at DESC
            LIMIT ?
        """, String.class, batch);

        int ok = 0;
        for (String code : codes) {
            try {
                var w = ghn.one(code);
                if (w != null) { store.upsertGhnOrderFromWhiteRow(w); ok++; }
            } catch (Exception ex) {
                log.warn("Backfill GHN {} fail: {}", code, ex.getMessage());
            }
        }
        return ok;
    }
}
