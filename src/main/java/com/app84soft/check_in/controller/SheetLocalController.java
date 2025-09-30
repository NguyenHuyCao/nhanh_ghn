package com.app84soft.check_in.controller;

import com.app84soft.check_in.services.sheet.BootstrapService;
import com.app84soft.check_in.services.sheet.GhnBackfillService;
import com.app84soft.check_in.services.sheet.LocalSheetQueryService;
import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/bootstrap")
    @Operation(summary = "Lưu trước N đơn mới nhất (từ Nhanh + enrich GHN)")
    public ResponseEntity<?> bootstrap(@RequestParam(defaultValue = "30") int limit) {
        int saved = bootstrap.bootstrapLatest(limit);
        return ResponseEntity.ok(Map.of("code", 200, "message", "bootstrapped", "saved", saved));
    }

    @GetMapping("/local")
    @Operation(summary = "Lấy dữ liệu từ DB (merge Nhanh + GHN)")
    public ResponseEntity<?> queryLocal(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean refresh
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
    @Operation(summary = "Backfill GHN (lấp dữ liệu GHN còn thiếu cho các đơn Nhanh đã có)")
    public ResponseEntity<?> backfill(@RequestParam(defaultValue = "200") int batch) {
        int ok = backfill.backfillMissing(batch);
        return ResponseEntity.ok(Map.of("code", 200, "filled", ok));
    }

    @PostMapping("/sync/trigger")
    @Operation(summary = "Gọi full-sync thủ công (không chờ trả về hết)")
    public ResponseEntity<?> triggerSync() {
        bootstrap.triggerFullSyncAsync();
        return ResponseEntity.ok(Map.of("code", 200, "message", "sync_started"));
    }
}
