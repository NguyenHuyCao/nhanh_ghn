package com.app84soft.check_in.other_service.nhanh;

import com.app84soft.check_in.dto.nhanh.NhanhProperties;
import com.app84soft.check_in.dto.nhanh.response.OrderSheetRowDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NhanhOrderService {

    private final RestTemplate rt;
    private final NhanhProperties props;
    private final NhanhAuthService auth;
    private static final ObjectMapper OM = new ObjectMapper();

    public List<OrderSheetRowDto> getOrders(LocalDate from, LocalDate to, int page, int limit) {
        Map<String, Object> raw = callOrderIndex(from, to, page, limit);

        // data
        Map<String, Object> data = asMap(raw.get("data"));

        // orders (luôn ép an toàn)
        List<Map<String, Object>> orders = asListOfMap(data.get("orders"));

        final int[] idx = {1};

        return orders.stream().flatMap(o -> {
            String created   = String.valueOf(o.getOrDefault("createdTime", ""));
            Long   idNhanh   = asLong(o.get("id"));
            String phone     = asStr(o.get("customerMobile"));
            String status    = asStr(o.get("status"));
            String pay       = asStr(o.get("paymentMethod"));
            String carrier   = asStr(o.get("carrierName"));
            String ship      = asStr(o.get("shipmentCode"));

            List<Map<String, Object>> items = asListOfMap(
                    firstNonNull(o.get("items"), o.get("orderItems"))
            );

            if (items.isEmpty()) {
                return List.of(
                        OrderSheetRowDto.builder()
                                .stt(idx[0]++)
                                .ngay(created)
                                .idNhanh(idNhanh)
                                .soDienThoaiKhach(phone)
                                .kenhThanhToan(pay)
                                .donViVanChuyen(carrier)
                                .maDonHangVanChuyen(ship)
                                .trangThaiTrenNhanh(status)
                                .build()
                ).stream();
            }

            return items.stream().map(it ->
                    OrderSheetRowDto.builder()
                            .stt(idx[0]++)
                            .ngay(created)
                            .idNhanh(idNhanh)
                            .soDienThoaiKhach(phone)
                            .maSanPham(asStr(firstNonNull(it.get("sku"), it.get("productCode"), it.get("productId"))))
                            .size(asStr(firstNonNull(it.get("size"), it.get("variantName"))))
                            .giaTienBan(asLong(firstNonNull(it.get("price"), it.get("sellPrice"), it.get("unitPrice"))))
                            .kenhThanhToan(pay)
                            .donViVanChuyen(carrier)
                            .maDonHangVanChuyen(ship)
                            .trangThaiTrenNhanh(status)
                            .build()
            );
        }).collect(Collectors.toList());
    }

    public Map<String, Object> debugRaw(LocalDate from, LocalDate to, int page, int limit) {
        return callOrderIndex(from, to, page, limit);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callOrderIndex(LocalDate from, LocalDate to, int page, int limit) {
        String url = trimSlash(props.getBaseUrl()) + "/api/order/index";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fromDate", from.toString());
        data.put("toDate",   to.toString());
        data.put("page",     page);
        data.put("limit",    limit);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("version", "2.0");
        form.add("appId", ""); // để trống nếu phía Nhanh không yêu cầu
        form.add("businessId", props.getBusinessId());
        form.add("accessToken", auth.getAccessToken());
        form.add("data", writeJsonSafe(data));

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> resp = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(form, h), Map.class);
        return resp.getBody() == null ? Collections.emptyMap() : (Map<String, Object>) resp.getBody();
    }

    /* ---------------- helpers (ép kiểu an toàn) ---------------- */

    private static Map<String, Object> asMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            // copy sang Map<String,Object> để IDE không cảnh báo
            Map<String, Object> res = new LinkedHashMap<>();
            m.forEach((k, v) -> res.put(String.valueOf(k), v));
            return res;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asListOfMap(Object o) {
        if (!(o instanceof List<?> list)) return Collections.emptyList();
        List<Map<String, Object>> res = new ArrayList<>(list.size());
        for (Object e : list) {
            if (e instanceof Map<?, ?> m) {
                Map<String, Object> mm = new LinkedHashMap<>();
                m.forEach((k, v) -> mm.put(String.valueOf(k), v));
                res.add(mm);
            }
        }
        return res;
    }

    private static String writeJsonSafe(Object o) {
        try { return OM.writeValueAsString(o); }
        catch (Exception e) { return "{}"; }
    }

    private static String trimSlash(String u) {
        if (u == null) return "";
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(String.valueOf(v)); }
        catch (Exception ignore) { return null; }
    }

    private static String asStr(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Object firstNonNull(Object... arr) {
        for (Object o : arr) if (o != null) return o;
        return null;
    }
}
