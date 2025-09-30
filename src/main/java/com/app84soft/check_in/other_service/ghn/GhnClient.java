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
import java.util.stream.Collectors;

/**
 * GHN client – chuẩn hoá header, retry hợp lý, lọc ShopId đang hoạt động (status=1),
 * tự dò ShopId cho chi tiết đơn và chống spam bằng negative-cache ngắn hạn.
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

    /** cache: map order_code -> shopId đã dò được (LRU đơn giản) */
    private final Map<String, Long> orderShopCache = new ConcurrentHashMap<>();

    /** cache danh sách shop id của token (chỉ lấy shop status=1) */
    private volatile List<Long> cachedActiveShopIds = Collections.emptyList();
    private static final long SHOP_LIST_TTL_MS = 10 * 60 * 1000L;
    private volatile long shopListExpireAt = 0L;

    /** negative cache: những order_code đã thử đủ shops nhưng không tìm thấy – để tránh quét lặp */
    private final Map<String, Long> notFoundCache = new ConcurrentHashMap<>();
    private static final long NOTFOUND_TTL_MS = 15 * 60 * 1000L; // 15 phút

    /* ================== Public APIs ================== */

    // Header chỉ có Token (cho /v2/shop/all)
    private HttpHeaders authHeadersTokenOnly() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Token", Objects.requireNonNull(props.getToken(), "GHN token is null"));
        return h;
    }

    /**
     * Giữ cho backward-compatible: đã chuyển sang multi-shop (tự bơm shop_ids).
     */
    @Deprecated
    public Map<String, Object> searchOrders(LocalDate from, LocalDate to, int page, int limit, String status) {
        String url = base("/v2/shipping-order/search");

        Map<String, Object> filter = new LinkedHashMap<>();
        if (from != null) filter.put("from_date", from.toString());
        if (to   != null) filter.put("to_date",   to.toString());
        if (status != null && !status.isBlank()) filter.put("status", status);

        // CHANGED: bơm tất cả shop đang hoạt động
        filter.put("shop_ids", getActiveShopIds());

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

    /** Search với body tuỳ biến – nếu không có shop_ids thì bơm active shops. */
    public Map<String, Object> listOrders(Map<String, Object> body) {
        String url = base("/v2/shipping-order/search");
        @SuppressWarnings("unchecked")
        Map<String, Object> filter = (Map<String, Object>) body.get("filter");
        if (filter == null) {
            filter = new LinkedHashMap<>();
            body.put("filter", filter);
        }
        if (!filter.containsKey("shop_ids")) {
            filter.put("shop_ids", getActiveShopIds()); // CHANGED
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
     * Bước 0: nếu negative-cache -> bỏ qua để tránh spam.
     * Bước 1: thử shop cache (đã dò lần trước).
     * Bước 2: thử shop mặc định.
     * Bước 3: quét các shop ACTIVE còn lại.
     * Nếu vẫn không có -> set negative-cache TTL.
     */
    public Optional<Map<String, Object>> getOrderDetail(String orderCode) {
        long now = System.currentTimeMillis();
        Long nf = notFoundCache.get(orderCode);
        if (nf != null && nf > now) {
            log.info("GHN.detail short-circuit NOTFOUND cache: order={}", orderCode);
            return Optional.empty();
        }

        long t0 = System.currentTimeMillis();

        // 0) cache shop theo order
        Long cachedShop = orderShopCache.get(orderCode);
        if (cachedShop != null) {
            log.info("GHN.detail use cached shop: order={} shopId={}", orderCode, cachedShop);
            Optional<Map<String, Object>> ok = post(DETAIL_URL, Map.of("order_code", orderCode), cachedShop);
            if (ok.isPresent()) return ok;
        }

        Long defaultShopId = props.getShopId();
        Map<String, Object> body = Map.of("order_code", orderCode);

        // 1) thử shop mặc định
        if (defaultShopId != null) {
            log.info("GHN.detail try default shop: order={} shopId={}", orderCode, defaultShopId);
            Optional<Map<String, Object>> r = post(DETAIL_URL, body, defaultShopId);
            if (r.isPresent()) {
                orderShopCache.put(orderCode, defaultShopId);
                log.info("GHN.detail OK with default shop: order={} shopId={} in {}ms",
                        orderCode, defaultShopId, System.currentTimeMillis() - t0);
                return r;
            }
        }

        // 2) quét các shop ACTIVE
        List<Long> shops = getActiveShopIds();
        List<Long> others = new ArrayList<>(shops.size());
        for (Long id : shops) if (!Objects.equals(id, defaultShopId)) others.add(id);

        if (others.isEmpty()) {
            log.info("GHN.detail no other ACTIVE shops to scan (token has only default shopId={}), order={}",
                    defaultShopId, orderCode);
            notFoundCache.put(orderCode, now + NOTFOUND_TTL_MS);
            return Optional.empty();
        }

        log.info("GHN.detail scanning {} ACTIVE shops for order-{}: {}", others.size(), orderCode, others);
        for (Long sid : others) {
            Optional<Map<String, Object>> rx = post(DETAIL_URL, body, sid);
            if (rx.isPresent()) {
                orderShopCache.put(orderCode, sid);
                log.info("GHN.detail OK after scan: order={} shopId={} in {}ms",
                        orderCode, sid, System.currentTimeMillis() - t0);
                return rx;
            }
        }

        log.warn("GHN.detail FAILED to resolve shop: order={} in {}ms", orderCode, System.currentTimeMillis() - t0);
        notFoundCache.put(orderCode, now + NOTFOUND_TTL_MS);
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

    /** Danh sách shop (để kiểm tra nhanh token). Chỉ trả ACTIVE (status=1). */
    public Map<String, Object> getShops() {
        String url = base("/v2/shop/all");
        try {
            ResponseEntity<Map> res =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(authHeadersTokenOnly()), Map.class);
            Map<String, Object> body = res.getBody() == null ? Map.of() : res.getBody();
            // Gắn thêm trường active_shop_ids để bạn dễ debug
            List<Long> active = extractActiveShopIds(body);
            return Map.of(
                    "code", body.getOrDefault("code", 200),
                    "message", body.getOrDefault("message", "OK"),
                    "data", body.get("data"),
                    "active_shop_ids", active
            );
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

    /**
     * Lấy toàn bộ ACTIVE shopId từ `/v2/shop/all` và cache 10 phút.
     * Nếu lỗi/fallback thì vẫn trả về shopId mặc định để hệ thống không chết.
     */
    public List<Long> getActiveShopIds() {
        long now = System.currentTimeMillis();
        if (now < shopListExpireAt && !cachedActiveShopIds.isEmpty()) {
            log.debug("GHN.shops ACTIVE cache HIT: {}", cachedActiveShopIds);
            return cachedActiveShopIds;
        }

        List<Long> ids = new ArrayList<>();
        try {
            ResponseEntity<Map> res =
                    restTemplate.exchange(base("/v2/shop/all"), HttpMethod.GET,
                            new HttpEntity<>(authHeadersTokenOnly()), Map.class);

            Map<String, Object> body = res.getBody();
            ids = extractActiveShopIds(body);
        } catch (Exception e) {
            log.warn("Load GHN shops failed: {}", e.getMessage());
        }

        if (ids.isEmpty()) {
            // fallback: ít nhất trả về shop mặc định để không vỡ flow
            Long d = props.getShopId();
            if (d != null) ids = List.of(d);
        }

        cachedActiveShopIds = ids.stream().distinct().collect(Collectors.toList());
        shopListExpireAt = now + SHOP_LIST_TTL_MS;
        log.info("GHN.shops cached (ACTIVE): {}", cachedActiveShopIds);
        return cachedActiveShopIds;
    }

    /** Trích ACTIVE shop id từ body /v2/shop/all (status == 1). */
    @SuppressWarnings("unchecked")
    private static List<Long> extractActiveShopIds(Map body) {
        if (body == null) return List.of();
        Object dataObj = body.get("data");
        if (!(dataObj instanceof Map<?,?> data)) return List.of();
        Object shopsObj = data.get("shops");
        if (!(shopsObj instanceof List<?> shops)) return List.of();
        List<Long> ids = new ArrayList<>();
        for (Object o : shops) {
            if (!(o instanceof Map<?,?> m)) continue;
            Object id = m.get("id");
            Object st = m.get("status"); // GHN thường trả 1=active, 0=inactive
            boolean active = true;
            try { active = st == null || Integer.parseInt(String.valueOf(st)) == 1; } catch (Exception ignore) {}
            if (!active) continue;
            if (id != null) {
                try { ids.add(Long.parseLong(String.valueOf(id))); } catch (NumberFormatException ignore) {}
            }
        }
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
