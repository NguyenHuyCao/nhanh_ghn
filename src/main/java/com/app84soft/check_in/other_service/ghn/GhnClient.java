package com.app84soft.check_in.other_service.ghn;

import com.app84soft.check_in.dto.ghn.GhnProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GHN client – chuẩn hoá header, retry hợp lý và auto dò ShopId cho chi tiết đơn.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GhnClient {

    private final RestTemplate restTemplate;
    private final GhnProperties props; // cần có: baseUrl, token, shopId

    // ==== GHN API endpoints (dùng khi cần gọi thẳng URL tuyệt đối) ====
    private static final String GHN_BASE   = "https://online-gateway.ghn.vn/shiip/public-api";
    private static final String DETAIL_URL = GHN_BASE + "/v2/shipping-order/detail";
    private static final String SEARCH_URL = GHN_BASE + "/v2/shipping-order/search";
    private static final String SHOPS_URL  = GHN_BASE + "/v2/shop/all";

    // cache: map order_code -> shopId đã dò được
    private final Map<String, Long> orderShopCache = new ConcurrentHashMap<>();
    // cache danh sách shop id của token
    private volatile List<Long> cachedShopIds = Collections.emptyList();
    private static final long SHOP_LIST_TTL_MS = 10 * 60 * 1000L;
    private volatile long shopListExpireAt = 0L;

    /* ================== Public APIs ================== */

    // ADD: header chỉ có Token (dùng cho /v2/shop/all)
    private HttpHeaders authHeadersTokenOnly() { // ADD
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Token", Objects.requireNonNull(props.getToken(), "GHN token is null"));
        return h;
    }

    /**
     * @deprecated không còn dùng trong flow; để tránh hỏng code cũ, vẫn giữ lại nhưng chuyển qua multi-shop.
     */
    @Deprecated // CHANGED: đánh dấu không dùng
    public Map<String, Object> searchOrders(LocalDate from, LocalDate to, int page, int limit, String status) { // CHANGED
        String url = base("/v2/shipping-order/search");

        Map<String, Object> filter = new LinkedHashMap<>();
        if (from != null) filter.put("from_date", from.toString());
        if (to   != null) filter.put("to_date",   to.toString());
        if (status != null && !status.isBlank()) filter.put("status", status);
        // CHANGED: thay vì 1 shop mặc định -> tất cả shop thuộc token
        filter.put("shop_ids", getAllShopIds());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page",  page  <= 0 ? 1  : page);
        body.put("limit", limit <= 0 ? 20 : limit);
        body.put("filter", filter);

        long t0 = System.nanoTime();
        Map<String, Object> rs = post(url, body).orElseGet(Map::of);
        log.info("GHN.searchOrders(from={} to={} page={} limit={} status={}) in {}ms code={}",
                from, to, page, limit, status, ms(t0), rs.get("code"));
        return rs;
    }

    /** Search với body tuỳ biến. */
    public Map<String, Object> listOrders(Map<String, Object> body) { // CHANGED
        String url = base("/v2/shipping-order/search");
        @SuppressWarnings("unchecked")
        Map<String, Object> filter = (Map<String, Object>) body.get("filter");
        if (filter == null) {
            filter = new LinkedHashMap<>();
            body.put("filter", filter);
        }
        // Nếu caller KHÔNG chỉ định shop_ids -> tự gắn tất cả shop của token
        if (!filter.containsKey("shop_ids")) {
            filter.put("shop_ids", getAllShopIds()); // CHANGED
        }

        int page  = asInt(body.get("page"), 1);
        int limit = asInt(body.get("limit"), 20);
        log.info("GHN.listOrders page={} limit={} shop_ids={}", page, limit, filter.get("shop_ids"));
        long t0 = System.nanoTime();
        var rs = post(url, body).orElseGet(Map::of);
        log.info("GHN.listOrders done in {}ms, code={}", ms(t0), rs.get("code"));
        return rs;
    }

    /**
     * Lấy chi tiết đơn GHN theo orderCode.
     * Bước 1: thử shop mặc định.
     * Bước 2: nếu fail và token có nhiều shop thì quét các shop còn lại.
     */
    public Optional<Map<String, Object>> getOrderDetail(String orderCode) {
        long t0 = System.currentTimeMillis();

        // Nếu đã dò được shop của order trước đó thì ưu tiên dùng ngay.
        Long cachedShop = orderShopCache.get(orderCode);
        if (cachedShop != null) {
            log.info("GHN.detail use cached shop: order={} shopId={}", orderCode, cachedShop);
            Optional<Map<String, Object>> ok = post(DETAIL_URL, Map.of("order_code", orderCode), cachedShop);
            if (ok.isPresent()) return ok;
        }

        Long defaultShopId = props.getShopId();
        Map<String, Object> body = Map.of("order_code", orderCode);

        // 1) thử shop mặc định
        log.info("GHN.detail try default shop: order={} shopId={}", orderCode, defaultShopId);
        Optional<Map<String, Object>> r = post(DETAIL_URL, body, defaultShopId);
        if (r.isPresent()) {
            orderShopCache.put(orderCode, defaultShopId);
            log.info("GHN.detail OK with default shop: order={} shopId={} in {}ms",
                    orderCode, defaultShopId, System.currentTimeMillis() - t0);
            return r;
        }

        // 2) nếu fail, quét các shop khác (nếu token có)
        List<Long> shops = getAllShopIds();
        List<Long> others = new ArrayList<>();
        for (Long id : shops) if (!Objects.equals(id, defaultShopId)) others.add(id);

        if (others.isEmpty()) {
            log.info("GHN.detail no other shops to scan (token only has shopId={}), order={}",
                    defaultShopId, orderCode);
            return Optional.empty();
        }

        log.info("GHN.detail scanning {} other shops for order-{}: {}", others.size(), orderCode, others);
        for (Long sid : others) {
            Optional<Map<String, Object>> rx = post(DETAIL_URL, body, sid);
            if (rx.isPresent()) {
                orderShopCache.put(orderCode, sid);
                log.info("GHN.detail OK after scan: order={} shopId={} in {}ms",
                        orderCode, sid, System.currentTimeMillis() - t0);
                return rx;
            }
        }

        log.warn("GHN.detail FAILED to resolve shop: order={} in {}ms",
                orderCode, System.currentTimeMillis() - t0);
        return Optional.empty();
    }

    public Map<String, Object> calculateFee(Map<String, Object> body) {
        String url = base("/v2/shipping-order/fee");
        long t0 = System.nanoTime();
        Map<String, Object> rs = post(url, body).orElseGet(Map::of);
        log.info("GHN.fee done in {}ms code={}", ms(t0), rs.get("code"));
        return rs;
    }

    public Map<String, Object> leadtime(Map<String, Object> body) {
        String url = base("/v2/shipping-order/leadtime");
        long t0 = System.nanoTime();
        Map<String, Object> rs = post(url, body).orElseGet(Map::of);
        log.info("GHN.leadtime done in {}ms code={}", ms(t0), rs.get("code"));
        return rs;
    }

    /** Danh sách shop (để kiểm tra nhanh token). */
    public Map<String, Object> getShops() { // CHANGED: dùng token-only
        String url = base("/v2/shop/all");
        try {
            ResponseEntity<Map> res =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeadersTokenOnly()), Map.class); // CHANGED
            return res.getBody() == null ? Map.of() : res.getBody();
        } catch (RestClientException ex) {
            log.warn("GHN.getShops failed: {}", ex.getMessage());
            return Map.of();
        }
    }

    public Map<String, Object> getProvinces() {
        String url = base("/master-data/province");
        try {
            ResponseEntity<Map> res =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return res.getBody() == null ? Map.of() : res.getBody();
        } catch (RestClientException ex) {
            log.warn("GHN.getProvinces failed: {}", ex.getMessage());
            return Map.of("code", 500, "message", "GHN getProvinces failed");
        }
    }

    public Map<String, Object> getDistricts(int provinceId) {
        String url = UriComponentsBuilder.fromHttpUrl(base("/master-data/district"))
                .queryParam("province_id", provinceId)
                .toUriString();
        try {
            ResponseEntity<Map> res =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return res.getBody() == null ? Map.of() : res.getBody();
        } catch (RestClientException ex) {
            log.warn("GHN.getDistricts failed: {}", ex.getMessage());
            return Map.of("code", 500, "message", "GHN getDistricts failed");
        }
    }

    public Map<String, Object> getWards(int districtId) {
        String url = UriComponentsBuilder.fromHttpUrl(base("/master-data/ward"))
                .queryParam("district_id", districtId)
                .toUriString();
        try {
            ResponseEntity<Map> res =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return res.getBody() == null ? Map.of() : res.getBody();
        } catch (RestClientException ex) {
            log.warn("GHN.getWards failed: {}", ex.getMessage());
            return Map.of("code", 500, "message", "GHN getWards failed");
        }
    }

    /* ================== Helpers ================== */

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> post(String url, Object body) {
        return doPost(url, body, props.getShopId());
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> post(String url, Object body, Long shopId) {
        return doPost(url, body, shopId);
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> doPost(String url, Object body, Long shopIdHeader) {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                HttpHeaders headers = authHeadersWithShop(shopIdHeader);
                log.debug("GHN POST {} attempt={} body={}", url, attempts, body);
                ResponseEntity<Map> res = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
                if (res.getStatusCode().is2xxSuccessful()) {
                    return Optional.ofNullable(res.getBody());
                }
                return Optional.empty();
            } catch (RestClientResponseException ex) {
                int status = ex.getRawStatusCode();
                String payload = ex.getResponseBodyAsString();
                log.warn("GHN POST {} failed (attempt {}): {} {}", url, attempts, status, payload);
                boolean transientErr = status >= 500 || containsTimeout(payload);
                if (!transientErr || attempts >= 2) return Optional.empty();
                sleepBackoff(attempts);
            } catch (RestClientException ex) {
                String msg = ex.getMessage() == null ? "" : ex.getMessage();
                log.warn("GHN POST {} failed (attempt {}): {}", url, attempts, msg);
                if (!containsTimeout(msg) || attempts >= 2) return Optional.empty();
                sleepBackoff(attempts);
            }
        }
    }

    private static boolean containsTimeout(String s) {
        if (s == null) return false;
        String m = s.toLowerCase(Locale.ROOT);
        return m.contains("timed out") || m.contains("timeout") || m.contains("deadline");
    }

    private static void sleepBackoff(int attempts) {
        try { Thread.sleep(300L * attempts); } catch (InterruptedException ignored) {}
    }

    /** Header mặc định từ cấu hình. */
    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Token", Objects.requireNonNull(props.getToken(), "GHN token is null"));
        h.set("ShopId", String.valueOf(Objects.requireNonNull(props.getShopId(), "GHN shopId is null")));
        return h;
    }

    /** Header với ShopId chỉ định. */
    private HttpHeaders authHeadersWithShop(Long shopId) {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Token", Objects.requireNonNull(props.getToken(), "GHN token is null"));
        h.set("ShopId", String.valueOf(Objects.requireNonNull(shopId, "GHN shopId is null")));
        return h;
    }

    /** Lấy toàn bộ shopId từ `/v2/shop/all` và cache 10 phút. */
    public List<Long> getAllShopIds() { // CHANGED: public + token-only
        long now = System.currentTimeMillis();
        if (now < shopListExpireAt && !cachedShopIds.isEmpty()) {
            log.debug("GHN.shops cache HIT: {}", cachedShopIds);
            return cachedShopIds;
        }

        List<Long> ids = new ArrayList<>();
        try {
            ResponseEntity<Map> res =
                    restTemplate.exchange(base("/v2/shop/all"), HttpMethod.GET,
                            new HttpEntity<>(authHeadersTokenOnly()), Map.class); // CHANGED

            Map<String, Object> body = res.getBody();
            if (body != null && body.get("data") instanceof Map<?, ?> data) {
                Object shopsObj = ((Map<?, ?>) data).get("shops");
                if (shopsObj instanceof List<?> shops) {
                    for (Object o : shops) {
                        if (o instanceof Map<?, ?> m) {
                            Object id = m.get("id");
                            if (id != null) {
                                try { ids.add(Long.parseLong(String.valueOf(id))); } catch (NumberFormatException ignore) {}
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Load GHN shops failed: {}", e.getMessage());
        }

        if (ids.isEmpty()) ids = List.of(props.getShopId()); // fallback
        cachedShopIds = ids;
        shopListExpireAt = now + SHOP_LIST_TTL_MS;
        log.info("GHN.shops cached (ALL): {}", ids);
        return ids;
    }

    /** Ghép base-url + path. */
    private String base(String path) {
        String b = props.getBaseUrl();
        if (b == null || b.isBlank()) {
            throw new IllegalStateException("GHN baseUrl is missing (ghn.api.base-url)");
        }
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (path == null) path = "";
        if (!path.startsWith("/")) path = "/" + path;
        return b + path;
    }

    /* ================== Startup checks ================== */
    @PostConstruct
    void verifyConfig() {
        if (props.getToken() == null || props.getToken().isBlank())
            throw new IllegalStateException("GHN token is missing (ghn.api.token)");
        if (props.getShopId() == null)
            throw new IllegalStateException("GHN shopId is missing (ghn.api.shop-id)");
        if (props.getBaseUrl() == null || props.getBaseUrl().isBlank())
            throw new IllegalStateException("GHN baseUrl is missing (ghn.api.base-url)");
        log.info("GHN ready: baseUrl={}, shopId={}", props.getBaseUrl(), props.getShopId());
    }

    /* ================== tiny utils ================== */
    private static int asInt(Object v, int def) {
        try { return v == null ? def : Integer.parseInt(String.valueOf(v)); }
        catch (Exception e) { return def; }
    }
    private static long ms(long t0){ return Math.round((System.nanoTime()-t0)/1_000_000.0); }
}
