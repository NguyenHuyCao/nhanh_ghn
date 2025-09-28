package com.app84soft.check_in.controller;

import com.app84soft.check_in.services.sheet.BootstrapService;
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

    /** 1) Lưu trước 30 đơn mới nhất (từ Nhanh + enrich GHN) */
    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(@RequestParam(defaultValue = "30") int limit) {
        int saved = bootstrap.bootstrapLatest(limit);
        return ResponseEntity.ok(Map.of("code", 200, "message", "bootstrapped", "saved", saved));
    }

    /** 2) Lấy dữ liệu từ DB (merge Nhanh + GHN) */
    @GetMapping("/local")
    public ResponseEntity<?> queryLocal(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        var pr = localQuery.query(from, to, page, limit);
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("total", pr.getTotal());
        data.put("page", pr.getPage());
        data.put("items", pr.getItems());
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    /** 3) Gọi full-sync thủ công (không chờ trả về hết) */
    @PostMapping("/sync/trigger")
    public ResponseEntity<?> triggerSync() {
        bootstrap.triggerFullSyncAsync();
        return ResponseEntity.ok(Map.of("code", 200, "message", "sync_started"));
    }
}
