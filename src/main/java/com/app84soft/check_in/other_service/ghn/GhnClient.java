package com.app84soft.check_in.other_service.ghn;

import com.app84soft.check_in.dto.ghn.GhnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GhnClient {

    private final RestTemplate restTemplate;
    private final GhnProperties props; // nhớ tạo class properties bên dưới

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        // BẮT BUỘC: GHN dùng header "Token"
        h.set("Token", props.getToken());
        // Một số API (v2, order/fee, order/detail, shop/all...) cần ShopId
        if (props.getShopId() != null) {
            h.set("ShopId", String.valueOf(props.getShopId()));
        }
        return h;
    }

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
    public Map<String, Object> getProvinces() {
        String url = base("/master-data/province");
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        return res.getBody();
    }

    public Map<String, Object> getDistricts(int provinceId) {
        String url = UriComponentsBuilder.fromHttpUrl(base("/master-data/district"))
                .queryParam("province_id", provinceId).toUriString();
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        return res.getBody();
    }

    public Map<String, Object> getWards(int districtId) {
        String url = UriComponentsBuilder.fromHttpUrl(base("/master-data/ward"))
                .queryParam("district_id", districtId).toUriString();
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        return res.getBody();
    }

    public Map<String, Object> getShops() {
        String url = base("/v2/shop/all");
        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        return res.getBody();
    }

    /* ---------- Orders ---------- */
    public Map<String, Object> listOrders(Map<String, Object> body) {
        String url = base("/v2/search-orders");
        ResponseEntity<Map> res =
                restTemplate.postForEntity(url, new HttpEntity<>(body, authHeaders()), Map.class);
        return res.getBody();
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
