// src/main/java/com/app84soft/check_in/intergration/NhanhExternalAdapter.java
package com.app84soft.check_in.intergration;

import com.app84soft.check_in.other_service.nhanh.NhanhClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class NhanhExternalAdapter implements ExternalNhanhClient {

    private final NhanhClient nhanhClient;

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Page<Order> listOrders(LocalDateTime from, LocalDateTime to, int page, int size) {
        try {
            Map<String,String> q = new LinkedHashMap<>();
            if (from != null) q.put("fromDate", D.format(from.toLocalDate()));
            if (to   != null) q.put("toDate",   D.format(to.toLocalDate().plusDays(1))); // end-exclusive
            q.put("page",  String.valueOf(page));
            q.put("limit", String.valueOf(size));

            Map<String,Object> resp = nhanhClient.listOrdersIndex(q);
            Map<String,Object> data = castMap(resp.get("data"));
            var items = extractOrders(data).stream().map(this::toOrder).toList();

            // nếu có totalPages thì suy ra last
            Integer totalPages = asInt(data.get("totalPages"));
            boolean last = (totalPages == null) || page >= totalPages;
            return new Page<>(items, last);
        } catch (Exception e) {
            return new Page<>(List.of(), true);
        }
    }

    @Override
    public Order getOrderById(long id) {
        // POS Nhanh không có “get by id” chuẩn public → fallback: gọi index theo keyword/id
        try {
            Map<String,String> q = new LinkedHashMap<>();
            q.put("keyword", String.valueOf(id));
            q.put("page", "1"); q.put("limit","1");
            Map<String,Object> resp = nhanhClient.listOrdersIndex(q);
            Map<String,Object> data = castMap(resp.get("data"));
            var list = extractOrders(data);
            if (list.isEmpty()) return null;
            return toOrder(list.get(0));
        } catch (Exception e) {
            return null;
        }
    }

    /* ------------ mapping helpers (tái sử dụng cách đọc ở BootstrapService) ------------ */
    private static Map<String,Object> castMap(Object o){
        if (o instanceof Map<?,?> m){ Map<String,Object> r=new LinkedHashMap<>(); m.forEach((k,v)-> r.put(String.valueOf(k),v)); return r;}
        return new LinkedHashMap<>();
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> extractOrders(Map<String,Object> data){
        Object ordersObj = data.get("orders")!=null? data.get("orders"): data.get("items");
        List<Map<String,Object>> out = new ArrayList<>();
        if (ordersObj instanceof Map<?,?> m) m.values().forEach(v -> out.add(castMap(v)));
        else if (ordersObj instanceof List<?> l) for (Object v: l) if (v instanceof Map<?,?> mv) out.add(castMap(mv));
        return out;
    }
    private static Integer asInt(Object o){ try { return o==null?null:Integer.valueOf(String.valueOf(o)); } catch(Exception e){ return null; } }

    private Order toOrder(Map<String,Object> o){
        long id = Long.parseLong(String.valueOf(o.get("id")));
        LocalDateTime createdAt = SheetTime.toLdt(o.get("createdDateTime"), o.get("createdTime"), o.get("createdAt"));
        String phone = str(first(o.get("customerMobile"), o.get("customerPhone")));
        String pay   = str(first(o.get("paymentMethod"), o.get("paymentMethodName")));
        Long cod     = lng(first(o.get("calcTotalMoney"), o.get("grandTotal"), o.get("total")));
        String carrier = str(first(o.get("carrierName"), o.get("shippingPartner")));
        String code    = str(first(o.get("carrierCode"), o.get("shipmentCode")));
        String status  = str(first(o.get("statusName"),  o.get("status")));

        var items = readItems(o).stream().map(it -> new Item(
                str(first(it.get("productCode"), it.get("sku"), it.get("productId"))),
                str(first(it.get("size"), it.get("variantName"))),
                lng(first(it.get("price"), it.get("sellPrice"), it.get("unitPrice"))),
                1
        )).toList();

        return new Order(id, createdAt, phone, isBlank(pay)?"COD":pay, cod, carrier, code, status, items);
    }
    private static List<Map<String,Object>> readItems(Map<String,Object> o){
        Object raw = Optional.ofNullable(o.get("products")).orElse(o.get("items"));
        if (!(raw instanceof List<?> l)) return List.of();
        List<Map<String,Object>> r = new ArrayList<>();
        for (Object e: l) if (e instanceof Map<?,?> m) r.add(castMap(m));
        return r;
    }
    private static Object first(Object... a){ for(Object x: a) if(x!=null) return x; return null; }
    private static boolean isBlank(String s){ return s==null || s.isBlank(); }
    private static String str(Object o){ return o==null? null : String.valueOf(o); }
    private static Long lng(Object o){ try { return o==null? null : Long.valueOf(String.valueOf(o)); } catch(Exception e){ return null; } }

    // tách chung để dùng lại
    static class SheetTime {
        static LocalDateTime toLdt(Object... candidates){
            for (Object c : candidates) {
                if (c==null) continue;
                String v = String.valueOf(c);
                try {
                    long n = Long.parseLong(v);
                    if (v.length() <= 10) n = n * 1000;
                    return new java.sql.Timestamp(n).toLocalDateTime();
                } catch (Exception ignore) {}
            }
            return null;
        }
    }
}
