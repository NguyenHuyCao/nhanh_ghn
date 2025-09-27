package com.app84soft.check_in.other_service.ghn;

import com.app84soft.check_in.dto.ghn.response.WhiteRowDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnSheetService {

    private final GhnClient ghn;

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]['Z']");

    public static class Page<T> {
        public final int page, limit, total;
        public final List<T> items;
        public Page(int page, int limit, int total, List<T> items) {
            this.page = page; this.limit = limit; this.total = total; this.items = items;
        }
    }

    /* ===== Simple in-memory cache (TTL) ===== */
    private static class CacheEntry<V> { final V v; final long exp; CacheEntry(V v,long exp){this.v=v;this.exp=exp;} }
    private final Map<String, CacheEntry<Page<WhiteRowDto>>> whiteCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<WhiteRowDto>> detailCache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 10 * 60 * 1000L; // 10 phút

    private static String cacheKeyWhite(LocalDate from, LocalDate to, int page, int limit) {
        return "white:" + (from==null?"null":from) + ":" + (to==null?"null":to) + ":" + page + ":" + limit;
    }

    @SuppressWarnings("unchecked")
    public Page<WhiteRowDto> white(LocalDate from, LocalDate to, int page, int limit) {
        String key = cacheKeyWhite(from, to, page, limit);
        long now = System.currentTimeMillis();
        CacheEntry<Page<WhiteRowDto>> hit = whiteCache.get(key);
        if (hit != null && hit.exp > now) return hit.v;

        Map<String, Object> searchBody = new LinkedHashMap<>();
        Map<String, Object> filter = new LinkedHashMap<>();
        if (from != null) filter.put("from_date", from.toString());
        if (to   != null) filter.put("to_date",   to.toString());
        searchBody.put("page",  page  <= 0 ? 1  : page);
        searchBody.put("limit", limit <= 0 ? 20 : limit);
        searchBody.put("filter", filter);

        Map<String, Object> res = ghn.listOrders(searchBody);
        Map<String, Object> data = map(res.get("data"));
        int total = asInt(data.get("total"), asInt(data.get("total_orders"), 0));

        List<Map<String, Object>> orders = listOfMap(first(data.get("orders"), data.get("items"), data.get("data")));
        List<WhiteRowDto> out = orders.stream().map(this::mapToWhite).collect(Collectors.toList());
        Page<WhiteRowDto> pageObj = new Page<>(page, limit, total, out);

        whiteCache.put(key, new CacheEntry<>(pageObj, now + TTL_MS));
        return pageObj;
    }

    /** Lấy chi tiết — có cache TTL */
    public WhiteRowDto one(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) return null;
        long now = System.currentTimeMillis();
        CacheEntry<WhiteRowDto> d = detailCache.get(orderCode);
        if (d != null && d.exp > now) return d.v;

        return ghn.getOrderDetail(orderCode).map(res -> {
            Map<String, Object> data = map(res.get("data"));
            if (data.isEmpty()) return null;

            String oc  = s(data.get("order_code"));
            String coc = s(first(data.get("client_order_code"), data.get("client_code"), data.get("order_client_code")));
            String status = normStatus(s(first(data.get("status"), data.get("current_status"))));

            Long cod = asLong(first(data.get("cod_amount"), data.get("cod_value"), data.get("cod")));
            if (!"Đã giao".equals(status)) cod = 0L;

            Long fee = asLong(first(data.get("total_fee"), data.get("fee")));
            if (fee == null) fee = 0L;

            LocalDateTime deliveredAt = parseTime(first(data.get("finish_date"), data.get("delivered_time"), data.get("updated_date")));

            WhiteRowDto dto = WhiteRowDto.builder()
                    .orderCode(oc)
                    .clientOrderCode(coc)
                    .deliveredAt(deliveredAt)
                    .shipFee(-Math.abs(fee))
                    .codAmount(cod == null ? 0L : cod)
                    .shipStatus(status)
                    .returnNote("Đã hoàn".equals(status) ? "Đã hoàn đơn" : "-")
                    .bankCollectedAt(null)
                    .bankAmount(null)
                    .build();

            detailCache.put(orderCode, new CacheEntry<>(dto, now + TTL_MS));
            return dto;
        }).orElse(null);
    }

    private WhiteRowDto mapToWhite(Map<String, Object> o) {
        String orderCode = s(o.get("order_code"));
        String clientOrderCode = s(first(o.get("client_order_code"), o.get("client_code"), o.get("order_client_code")));
        String status = normStatus(s(first(o.get("status"), o.get("current_status"))));

        Long cod = asLong(first(o.get("cod_amount"), o.get("cod_value"), o.get("cod")));
        if (!"Đã giao".equals(status)) cod = 0L;

        Long fee = asLong(first(o.get("total_fee"), o.get("fee")));
        if (fee == null) {
            log.debug("GHN fee is null for {}, set 0 to avoid detail fanout", orderCode);
            fee = 0L;
        }

        LocalDateTime deliveredAt = parseTime(first(o.get("finish_time"), o.get("delivered_time"), o.get("updated_date")));

        return WhiteRowDto.builder()
                .orderCode(orderCode)
                .clientOrderCode(clientOrderCode)
                .deliveredAt(deliveredAt)
                .shipFee(-Math.abs(fee))
                .codAmount(cod == null ? 0L : cod)
                .shipStatus(status)
                .returnNote("Đã hoàn".equals(status) ? "Đã hoàn đơn" : "-")
                .bankCollectedAt(null)
                .bankAmount(null)
                .build();
    }

    private static String normStatus(String s) {
        if (s == null) return "Mới tạo";
        String x = s.toLowerCase(Locale.ROOT);
        if (x.contains("delivered") || x.contains("success")) return "Đã giao";
        if (x.contains("return")) return "Đã hoàn";
        if (x.contains("pickup") || x.contains("storing") || x.contains("transport") || x.contains("sorting") || x.contains("delivering"))
            return "Đang giao";
        return "Mới tạo";
    }
    private static LocalDateTime parseTime(Object v) {
        if (v == null) return null;
        try {
            String s = String.valueOf(v);
            if (s.matches("\\d+")) {
                long n = Long.parseLong(s);
                if (s.length() <= 10) n *= 1000;
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(n), ZoneId.systemDefault());
            }
            return LocalDateTime.parse(s, ISO);
        } catch (Exception ignore) { return null; }
    }
    private static Object first(Object... arr) { for (Object e : arr) if (e != null) return e; return null; }
    private static Map<String, Object> map(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> r = new LinkedHashMap<>();
            m.forEach((k, v) -> r.put(String.valueOf(k), v));
            return r;
        }
        return new LinkedHashMap<>();
    }
    private static List<Map<String, Object>> listOfMap(Object o) {
        if (!(o instanceof List<?> l)) return List.of();
        List<Map<String, Object>> r = new ArrayList<>(l.size());
        for (Object e : l) if (e instanceof Map<?, ?> m) r.add(map(m));
        return r;
    }
    private static String s(Object o) { return o == null ? null : String.valueOf(o); }
    private static Integer asInt(Object o, int def) { try { return o == null ? def : Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; } }
    private static Long asLong(Object o) { try { return o == null ? null : Long.parseLong(String.valueOf(o)); } catch (Exception e) { return null; } }
}
