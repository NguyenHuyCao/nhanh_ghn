package com.app84soft.check_in.other_service.ghn;

import com.app84soft.check_in.dto.ghn.GhnProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class GhnClient {

    private final RestTemplate restTemplate;
    private final GhnProperties props;

    public Map<String, Object> searchOrders(LocalDate from, LocalDate to, int page, int limit, String status) {
        String url = base("/v2/shipping-order/search");

        Map<String, Object> filter = new LinkedHashMap<>();
        if (from != null) filter.put("from_date", from.toString());
        if (to   != null) filter.put("to_date",   to.toString());
        if (status != null && !status.isBlank()) filter.put("status", status);
        if (props.getShopId() != null) filter.put("shop_ids", List.of(props.getShopId()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page",  page <= 0 ? 1 : page);
        body.put("limit", limit <= 0 ? 20 : limit);
        body.put("filter", filter);

        return post(url, body).orElseGet(Map::of);
    }

    public Map<String, Object> listOrders(Map<String, Object> body) {
        String url = base("/v2/shipping-order/search");
        return post(url, body).orElseGet(Map::of);
    }

    public Map<String, Object> getProvinces() {
        String url = base("/master-data/province");
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return res.getBody();
        } catch (RestClientException ex) {
            log.warn("GHN getProvinces failed: {}", ex.getMessage());
            return Map.of("code", 500, "message", "GHN getProvinces failed");
        }
    }

    public Map<String, Object> getDistricts(int provinceId) {
        String url = UriComponentsBuilder.fromHttpUrl(base("/master-data/district"))
                .queryParam("province_id", provinceId)
                .toUriString();
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return res.getBody();
        } catch (RestClientException ex) {
            log.warn("GHN getDistricts failed: {}", ex.getMessage());
            return Map.of("code", 500, "message", "GHN getDistricts failed");
        }
    }

    public Map<String, Object> getWards(int districtId) {
        String url = UriComponentsBuilder.fromHttpUrl(base("/master-data/ward"))
                .queryParam("district_id", districtId)
                .toUriString();
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return res.getBody();
        } catch (RestClientException ex) {
            log.warn("GHN getWards failed: {}", ex.getMessage());
            return Map.of("code", 500, "message", "GHN getWards failed");
        }
    }

    /** Trả Optional để caller tự xử lý khi GHN timeout/400… */
    public Optional<Map<String, Object>> getOrderDetail(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) return Optional.empty();
        String url = base("/v2/shipping-order/detail");
        Map<String, Object> body = Map.of("order_code", orderCode);
        return post(url, body);
    }

    public Map<String, Object> calculateFee(Map<String, Object> body) {
        String url = base("/v2/shipping-order/fee");
        return post(url, body).orElseGet(Map::of);
    }

    public Map<String, Object> leadtime(Map<String, Object> body) {
        String url = base("/v2/shipping-order/leadtime");
        return post(url, body).orElseGet(Map::of);
    }

    public Map<String, Object> getShops() {
        String url = base("/v2/shop/all");
        try {
            ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return res.getBody() == null ? Map.of() : res.getBody();
        } catch (RestClientException ex) {
            log.warn("GHN getShops failed: {}", ex.getMessage());
            return Map.of();
        }
    }

    /* ----------------- helpers ----------------- */

    private Optional<Map<String, Object>> post(String url, Object body) {
        // retry ngắn 2 lần khi lỗi tạm (timeout/5xx/400 GHN timeout message)
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                ResponseEntity<Map> res = restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class);
                Map<String, Object> b = res.getBody();
                return Optional.ofNullable(b);
            } catch (RestClientException ex) {
                String msg = ex.getMessage() == null ? "" : ex.getMessage();
                boolean maybeTransient =
                        msg.contains("timed out") ||
                                msg.contains("timeout") ||
                                msg.contains("context deadline exceeded") ||
                                msg.contains("503") || msg.contains("502") || msg.contains("504");
                log.warn("GHN POST {} failed (attempt {}): {}", url, attempts, msg);
                if (attempts >= 2 || !maybeTransient) {
                    return Optional.empty();
                }
                try { Thread.sleep(300L * attempts); } catch (InterruptedException ignored) {}
            }
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Token", props.getToken());
        if (props.getShopId() != null) h.set("ShopId", String.valueOf(props.getShopId()));
        return h;
    }

    private String base(String path) {
        String b = props.getBaseUrl();
        if (b == null || b.isBlank()) b = "https://dev-online-gateway.ghn.vn/shiip/public-api";
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (path == null) path = "";
        if (!path.startsWith("/")) path = "/" + path;
        return b + path;
    }
}
