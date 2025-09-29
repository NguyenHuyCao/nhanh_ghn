package com.app84soft.check_in.controller;

import com.app84soft.check_in.services.sheet.BootstrapService;
import com.app84soft.check_in.services.sheet.GhnBackfillService;
import com.app84soft.check_in.services.sheet.LocalSheetQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/v1/sheet")
@RequiredArgsConstructor
public class SheetLocalController {

    private final BootstrapService bootstrap;
    private final LocalSheetQueryService localQuery;
    private final GhnBackfillService backfill;

    /** 1) Lưu trước 30 đơn mới nhất (từ Nhanh + enrich GHN) */
    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(@RequestParam(defaultValue = "30") int limit) {
        int saved = bootstrap.bootstrapLatest(limit);
        return ResponseEntity.ok(Map.of("code", 200, "message", "bootstrapped", "saved", saved));
    }

    /** 2) Lấy dữ liệu từ DB (merge Nhanh + GHN) */
    // SheetLocalController.java
    @GetMapping("/local")
    public ResponseEntity<?> queryLocal(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean refresh // đổi mặc định = false
    ) {
        if (localQuery.countAllOrders() == 0) {
            bootstrap.bootstrapLatest(200); // seed lần đầu
        } else if (refresh && page == 1) {
            // chạy không chặn
            bootstrap.refreshLatestAsync(100);
        }

        var pr = localQuery.query(from, to, page, limit);
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("total", pr.getTotal());
        data.put("page", pr.getPage());
        data.put("items", pr.getItems());
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @PostMapping("/ghn/backfill")
    public ResponseEntity<?> backfill(@RequestParam(defaultValue = "200") int batch) {
        int ok = backfill.backfillMissing(batch);
        return ResponseEntity.ok(Map.of("code", 200, "filled", ok));
    }

    public ResponseEntity<?> backfill(@RequestParam(defaultValue = "200") int batch,
                                      GhnBackfillService svc) {
        int ok = svc.backfillMissing(batch);
        return ResponseEntity.ok(Map.of("code", 200, "filled", ok));
    }


//    @PostMapping("/sync/window")
//    public ResponseEntity<?> syncWindow(@RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate from,
//                                        @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate to,
//                                        @RequestParam(defaultValue="100") int pageSize) {
//        bootstrap.syncWindow(from, to, pageSize); // triển khai giống triggerFullSyncAsync nhưng chạy sync (không @Async)
//        return ResponseEntity.ok(Map.of("code", 200, "message", "synced"));
//    }


    /** 3) Gọi full-sync thủ công (không chờ trả về hết) */
    @PostMapping("/sync/trigger")
    public ResponseEntity<?> triggerSync() {
        bootstrap.triggerFullSyncAsync();
        return ResponseEntity.ok(Map.of("code", 200, "message", "sync_started"));
    }
}
