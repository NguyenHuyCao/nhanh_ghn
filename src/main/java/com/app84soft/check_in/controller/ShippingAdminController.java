package com.app84soft.check_in.controller;

import com.app84soft.check_in.other_service.ghn.GhnClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/v1/shipping/ghn")
@RequiredArgsConstructor
@Tag(name = "GHN Admin")
public class ShippingAdminController {
    private final GhnClient ghnClient;

    @GetMapping("/provinces")
    @Operation(summary = "GHN - danh sách tỉnh/thành")
    public ResponseEntity<?> provinces() { return ResponseEntity.ok(ghnClient.getProvinces()); }

    @GetMapping("/districts")
    @Operation(summary = "GHN - danh sách quận/huyện")
    public ResponseEntity<?> districts(@RequestParam("province_id") int provinceId) {
        return ResponseEntity.ok(ghnClient.getDistricts(provinceId));
    }

    @GetMapping("/wards")
    @Operation(summary = "GHN - danh sách phường/xã")
    public ResponseEntity<?> wards(@RequestParam("district_id") int districtId) {
        return ResponseEntity.ok(ghnClient.getWards(districtId));
    }

    @GetMapping("/shops")
    @Operation(summary = "GHN - danh sách shop")
    public ResponseEntity<?> shops() { return ResponseEntity.ok(ghnClient.getShops()); }

    @PostMapping("/orders/search")
    @Operation(summary = "GHN - tìm kiếm đơn")
    public ResponseEntity<?> searchOrders(@RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(ghnClient.listOrders(body));
    }

    @GetMapping("/orders/{orderCode}")
    @Operation(summary = "GHN - chi tiết đơn")
    public ResponseEntity<?> getOrder(@PathVariable String orderCode) {
        return ResponseEntity.ok(ghnClient.getOrderDetail(orderCode));
    }

    @PostMapping("/fee")
    @Operation(summary = "GHN - tính phí")
    public ResponseEntity<?> fee(@RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(ghnClient.calculateFee(body));
    }

    @PostMapping("/leadtime")
    @Operation(summary = "GHN - lead time")
    public ResponseEntity<?> leadtime(@RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(ghnClient.leadtime(body));
    }
}
