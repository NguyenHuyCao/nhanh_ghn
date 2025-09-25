package com.app84soft.check_in.other_service.ghn;

import com.app84soft.check_in.dto.ghn.response.WhiteRowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GhnSheetService {

    private final GhnClient ghn;

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]['Z']");

    public static class Page<T> {
        public final int page, limit, total;
        public final List<T> items;

        public Page(int page, int limit, int total, List<T> items) {
            this.page = page;
            this.limit = limit;
            this.total = total;
            this.items = items;
        }
    }

    /** Bảng trắng: gom từ GHN search-orders (+detail khi cần) */
    @SuppressWarnings("unchecked")
    public Page<WhiteRowDto> white(LocalDate from, LocalDate to, int page, int limit) {
        Map<String, Object> searchBody = new LinkedHashMap<>();
        Map<String, Object> filter = new LinkedHashMap<>();
        if (from != null)
            filter.put("from_date", from.toString());
        if (to != null)
            filter.put("to_date", to.toString());
        searchBody.put("page", page <= 0 ? 1 : page);
        searchBody.put("limit", limit <= 0 ? 20 : limit);
        searchBody.put("filter", filter);

        Map<String, Object> res = ghn.listOrders(searchBody);
        Map<String, Object> data = map(res.get("data"));
        int total = asInt(data.get("total"), asInt(data.get("total_orders"), 0));

        List<Map<String, Object>> orders = listOfMap(data.get("orders"));
        if (orders.isEmpty())
            orders = listOfMap(data.get("items"));
        if (orders.isEmpty())
            orders = listOfMap(data.get("data"));

        List<WhiteRowDto> out =
                orders.stream().map(o -> mapToWhite(o)).collect(Collectors.toList());
        return new Page<>(page, limit, total, out);
    }

    /* ---------- mapping GHN -> WhiteRow ---------- */

    private WhiteRowDto mapToWhite(Map<String, Object> o) {
        String orderCode = s(o.get("order_code"));
        String clientOrderCode = s(first(o.get("client_order_code"), o.get("client_code"),
                o.get("order_client_code")));

        String rawStatus = s(first(o.get("status"), o.get("current_status")));
        String status = normStatus(rawStatus);

        // COD:
        Long cod = asLong(first(o.get("cod_amount"), o.get("cod_value"), o.get("cod")));
        if (!"Đã giao".equals(status))
            cod = 0L;

        Long fee = asLong(first(o.get("total_fee"), o.get("fee")));
        if (fee == null) {
            Map<String, Object> detail = ghn.getOrderDetail(orderCode);
            Map<String, Object> dd = map(detail.get("data"));
            fee = asLong(first(dd.get("total_fee"), dd.get("fee")));
        }
        if (fee == null)
            fee = 0L;

        LocalDateTime deliveredAt = parseTime(
                first(o.get("finish_time"), o.get("delivered_time"), o.get("updated_date")));

        return WhiteRowDto.builder().orderCode(orderCode).clientOrderCode(clientOrderCode)
                .deliveredAt(deliveredAt).shipFee(-Math.abs(fee)).codAmount(cod == null ? 0L : cod)
                .shipStatus(status).returnNote("Đã hoàn".equals(status) ? "Đã hoàn đơn" : "-")
                .bankCollectedAt(null).bankAmount(null).build();
    }

    /* ---------- helpers ---------- */

    private static String normStatus(String s) {
        if (s == null)
            return "Mới tạo";
        String x = s.toLowerCase(Locale.ROOT);
        if (x.contains("delivered") || x.contains("success"))
            return "Đã giao";
        if (x.contains("return"))
            return "Đã hoàn";
        if (x.contains("pickup") || x.contains("storing") || x.contains("transport")
                || x.contains("sorting") || x.contains("delivering"))
            return "Đang giao";
        return "Mới tạo";
    }

    private static LocalDateTime parseTime(Object v) {
        if (v == null)
            return null;
        try {
            String s = String.valueOf(v);
            if (s.matches("\\d+")) { // epoch seconds/millis
                long n = Long.parseLong(s);
                if (s.length() <= 10)
                    n *= 1000;
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(n), ZoneId.systemDefault());
            }
            return LocalDateTime.parse(s, ISO);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static Object first(Object... arr) {
        for (Object e : arr)
            if (e != null)
                return e;
        return null;
    }

    private static Map<String, Object> map(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> r = new LinkedHashMap<>();
            m.forEach((k, v) -> r.put(String.valueOf(k), v));
            return r;
        }
        return new LinkedHashMap<>();
    }

    private static List<Map<String, Object>> listOfMap(Object o) {
        if (!(o instanceof List<?> l))
            return List.of();
        List<Map<String, Object>> r = new ArrayList<>(l.size());
        for (Object e : l)
            if (e instanceof Map<?, ?> m)
                r.add(map(m));
        return r;
    }

    private static String s(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer asInt(Object o, int def) {
        try {
            return o == null ? def : Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private static Long asLong(Object o) {
        try {
            return o == null ? null : Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
