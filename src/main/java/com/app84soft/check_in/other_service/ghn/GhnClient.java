package com.app84soft.check_in.other_service.ghn;

import com.app84soft.check_in.dto.ghn.GhnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GhnClient {

    private final RestTemplate restTemplate;
    private final GhnProperties props; // nhớ tạo class properties bên dưới

    public Map<String, Object> searchOrders(LocalDate from, LocalDate to, int page, int limit,
            String status) {
        String url = base("/v2/shipping-order/search");
        Map<String, Object> filter = new LinkedHashMap<>();
        if (from != null)
            filter.put("from_date", from.toString());
        if (to != null)
            filter.put("to_date", to.toString());
        if (status != null && !status.isBlank())
            filter.put("status", status);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page", page <= 0 ? 1 : page);
        body.put("limit", limit <= 0 ? 20 : limit);
        body.put("filter", filter);

        return restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class)
                .getBody();
    }


    // private HttpHeaders authHeaders() {
    // HttpHeaders h = new HttpHeaders();
    // h.setAccept(List.of(MediaType.APPLICATION_JSON));
    // h.setContentType(MediaType.APPLICATION_JSON);
    // h.set("Token", props.getToken());
    // if (props.getShopId() != null) {
    // h.set("ShopId", String.valueOf(props.getShopId()));
    // }
    // return h;
    // }

    private String base(String path) {
        String b = props.getBaseUrl();
        if (b == null || b.isBlank()) {
            // fallback môi trường dev của GHN
            b = "https://dev-online-gateway.ghn.vn/shiip/public-api";
        }
        if (b.endsWith("/"))
            b = b.substring(0, b.length() - 1);
        if (path == null)
            path = "";
        if (!path.startsWith("/"))
            path = "/" + path;
        return b + path;
    }


    /* ---------- Master data ---------- */
    /* ---------- Master data (POST) ---------- */
    public Map<String, Object> getProvinces() {
        String url = base("/master-data/province");
        return restTemplate.postForEntity(url, new HttpEntity<>(Map.of(), authHeaders()), Map.class)
                .getBody();
    }

    public Map<String, Object> getDistricts(int provinceId) {
        String url = base("/master-data/district");
        Map<String, Object> body = Map.of("province_id", provinceId);
        return restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class)
                .getBody();
    }

    public Map<String, Object> getWards(int districtId) {
        String url = base("/master-data/ward");
        Map<String, Object> body = Map.of("district_id", districtId);
        return restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class)
                .getBody();
    }

    /* ---------- Orders ---------- */

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Token", props.getToken()); // GHN bắt buộc
        if (props.getShopId() != null)
            h.set("ShopId", String.valueOf(props.getShopId()));
        return h;
    }

    public Map<String, Object> getShops() {
        String url = base("/v2/shop/all");
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        return res.getBody();
    }

    /* ---------- Orders ---------- */
    public Map<String, Object> listOrders(Map<String, Object> body) {
        String url = base("/v2/shipping-order/search");
        return restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class)
                .getBody();
    }

    public Map<String, Object> getOrderDetail(String orderCode) {
        String url = base("/v2/shipping-order/detail");
        Map<String, Object> body = Map.of("order_code", orderCode);
        ResponseEntity<Map> res =
                restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class);
        return res.getBody();
    }

    /* ---------- Fee & leadtime ---------- */
    public Map<String, Object> calculateFee(Map<String, Object> body) {
        String url = base("/v2/shipping-order/fee");
        ResponseEntity<Map> res =
                restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class);
        return res.getBody();
    }

    public Map<String, Object> leadtime(Map<String, Object> body) {
        String url = base("/v2/shipping-order/leadtime");
        ResponseEntity<Map> res =
                restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class);
        return res.getBody();
    }
}
