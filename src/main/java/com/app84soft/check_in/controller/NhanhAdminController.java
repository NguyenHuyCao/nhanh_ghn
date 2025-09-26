package com.app84soft.check_in.controller;

import com.app84soft.check_in.dto.nhanh.response.OrderYellowRowDto;
import com.app84soft.check_in.dto.response.PageResult;
import com.app84soft.check_in.other_service.nhanh.NhanhAuthService;
import com.app84soft.check_in.other_service.nhanh.NhanhSheetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/v1/nhanh")
@RequiredArgsConstructor
@Tag(name = "Nhanh Admin")
public class NhanhAdminController {

    private final NhanhAuthService auth;
    private final NhanhSheetService sheet;

    /* ===================== Helpers ===================== */

    private static LocalDate[] resolveLocalDateRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) return new LocalDate[]{null, null};
        LocalDate today = LocalDate.now();
        LocalDate f = (from == null) ? LocalDate.of(2000, 1, 1) : from;
        LocalDate t = (to == null) ? today : to;
        if (f.isAfter(t)) { LocalDate tmp = f; f = t; t = tmp; }
        return new LocalDate[]{f, t};
    }

    private static ResponseEntity<Map<String, Object>> okMinimal(int page, long total, List<?> items) {
        if (items != null && page < 1) page = 1;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("items", items == null ? List.of() : items);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 200);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    private static long parseLongSafe(Object v) {
        try { return (v == null) ? 0L : Long.parseLong(String.valueOf(v)); }
        catch (Exception ignore) { return 0L; }
    }

    private static <T> List<T> clampByLimit(List<T> in, int limit) {
        if (in == null) return List.of();
        if (limit <= 0 || in.size() <= limit) return in;
        return in.subList(0, limit);
    }

    /* ===================== SHEET (Yellow) ===================== */

    @GetMapping("/sheet/")
    @Operation(summary = "Bảng sheet")
    public ResponseEntity<Map<String, Object>> yellow(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws JsonProcessingException {

        Date fromD = (from == null && to == null) ? null : (from == null ? null : java.sql.Date.valueOf(from));
        Date toD   = (from == null && to == null) ? null : (to   == null ? null : java.sql.Date.valueOf(to));

        PageResult<OrderYellowRowDto> pr = sheet.getYellowPage(fromD, toD, page, limit);
        List<OrderYellowRowDto> items = clampByLimit(pr.getItems(), limit);
        return okMinimal(pr.getPage(), pr.getTotal(), items);
    }

    /* ===================== PRODUCTS ===================== */

    @GetMapping("/products")
    @Operation(summary = "Sản phẩm")
    public ResponseEntity<Map<String, Object>> products(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws JsonProcessingException {

        LocalDate[] rr = resolveLocalDateRange(from, to);
        Date fromD = rr[0] == null ? null : java.sql.Date.valueOf(rr[0]);
        Date toD   = rr[1] == null ? null : java.sql.Date.valueOf(rr[1]);

        var items = clampByLimit(sheet.getProducts(fromD, toD, page, limit), limit);
        var raw   = sheet.getRaw(fromD, toD, page, limit);
        var data  = (Map<String, Object>) raw.getOrDefault("data", Collections.emptyMap());
        long total = parseLongSafe(data.get("totalRecords"));
        return okMinimal(page, total, items);
    }

    /* ===================== PAYMENTS ===================== */

    @GetMapping("/payments")
    @Operation(summary = "Thanh toán/COD")
    public ResponseEntity<Map<String, Object>> payments(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws JsonProcessingException {

        LocalDate[] rr = resolveLocalDateRange(from, to);
        Date fromD = rr[0] == null ? null : java.sql.Date.valueOf(rr[0]);
        Date toD   = rr[1] == null ? null : java.sql.Date.valueOf(rr[1]);

        var items = clampByLimit(sheet.getPayments(fromD, toD, page, limit), limit);
        var raw   = sheet.getRaw(fromD, toD, page, limit);
        var data  = (Map<String, Object>) raw.getOrDefault("data", Collections.emptyMap());
        long total = parseLongSafe(data.get("totalRecords"));
        return okMinimal(page, total, items);
    }

    /* ===================== SHIPPING ===================== */

    @GetMapping("/shipping")
    @Operation(summary = "Giao vận")
    public ResponseEntity<Map<String, Object>> shipping(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws JsonProcessingException {

        LocalDate[] rr = resolveLocalDateRange(from, to);
        Date fromD = rr[0] == null ? null : java.sql.Date.valueOf(rr[0]);
        Date toD   = rr[1] == null ? null : java.sql.Date.valueOf(rr[1]);

        var items = clampByLimit(sheet.getShipping(fromD, toD, page, limit), limit);
        var raw   = sheet.getRaw(fromD, toD, page, limit);
        var data  = (Map<String, Object>) raw.getOrDefault("data", Collections.emptyMap());
        long total = parseLongSafe(data.get("totalRecords"));
        return okMinimal(page, total, items);
    }

    /* ===================== CUSTOMER ===================== */

    @GetMapping("/customer")
    @Operation(summary = "Khách hàng")
    public ResponseEntity<Map<String, Object>> customer(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws JsonProcessingException {

        LocalDate[] rr = resolveLocalDateRange(from, to);
        Date fromD = rr[0] == null ? null : java.sql.Date.valueOf(rr[0]);
        Date toD   = rr[1] == null ? null : java.sql.Date.valueOf(rr[1]);

        var items = clampByLimit(sheet.getCustomer(fromD, toD, page, limit), limit);
        var raw   = sheet.getRaw(fromD, toD, page, limit);
        var data  = (Map<String, Object>) raw.getOrDefault("data", Collections.emptyMap());
        long total = parseLongSafe(data.get("totalRecords"));
        return okMinimal(page, total, items);
    }

    /* ===================== META ===================== */

    @GetMapping("/meta")
    @Operation(summary = "Meta filter")
    public ResponseEntity<Map<String, Object>> meta(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws JsonProcessingException {

        LocalDate[] rr = resolveLocalDateRange(from, to);
        Date fromD = rr[0] == null ? null : java.sql.Date.valueOf(rr[0]);
        Date toD   = rr[1] == null ? null : java.sql.Date.valueOf(rr[1]);

        var item  = sheet.getMeta(fromD, toD, page, limit);
        var items = List.of(item);

        var raw   = sheet.getRaw(fromD, toD, page, limit);
        var data  = (Map<String, Object>) raw.getOrDefault("data", Collections.emptyMap());
        long total = parseLongSafe(data.get("totalRecords"));
        return okMinimal(page, total, items);
    }

    /* ===================== SUMMARY ===================== */

    @GetMapping("/summary")
    @Operation(summary = "Tổng hợp sheet")
    public ResponseEntity<Map<String, Object>> summary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws JsonProcessingException {

        LocalDate[] rr = resolveLocalDateRange(from, to);
        Date fromD = rr[0] == null ? null : java.sql.Date.valueOf(rr[0]);
        Date toD   = rr[1] == null ? null : java.sql.Date.valueOf(rr[1]);

        var item  = sheet.getSummary(fromD, toD, page, limit);
        var items = List.of(item);

        var raw   = sheet.getRaw(fromD, toD, page, limit);
        var data  = (Map<String, Object>) raw.getOrDefault("data", Collections.emptyMap());
        long total = parseLongSafe(data.get("totalRecords"));
        return okMinimal(page, total, items);
    }

    /* ---------- TOKEN ---------- */

    @GetMapping("/token")
    @Operation(summary = "Access token POS")
    public ResponseEntity<Map<String, Object>> token() {
        Map<String, Object> data = Map.of("accessToken", auth.getAccessToken());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 200);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }
}
