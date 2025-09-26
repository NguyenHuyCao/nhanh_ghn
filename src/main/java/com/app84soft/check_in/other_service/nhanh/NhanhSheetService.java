package com.app84soft.check_in.other_service.nhanh;

import com.app84soft.check_in.dto.nhanh.response.*;
import com.app84soft.check_in.dto.response.PageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gom dữ liệu “Bảng vàng” từ POS Nhanh (/api/order/index) và các nhóm dữ liệu con.
 * - Filter ngày dùng yyyy-MM-dd; toDate gửi end + 1 day (do POS end-exclusive).
 * - Khoảng lớn: chia nhỏ theo cửa sổ (MAX_DAYS_PER_CALL) + phân trang chính xác theo tổng thực.
 */
@Service
@RequiredArgsConstructor
public class NhanhSheetService {

    private final NhanhClient nhanhClient;

    /* ========================= Formats & Config ========================= */
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final java.text.SimpleDateFormat D_FMT = new java.text.SimpleDateFormat("yyyy-MM-dd");

    /** Số ngày tối đa mỗi cửa sổ gọi POS */
    private static final int MAX_DAYS_PER_CALL = 7;

    /** Mặc định khi from/to null */
    private static final LocalDate MIN_DATE_FOR_ALL = LocalDate.of(2000, 1, 1);

    /* ========================= PAGE (controller dùng) ========================= */

    public PageResult<OrderYellowRowDto> getYellowPage(Date from, Date to, int page, int limit)
            throws JsonProcessingException {

        WindowFetchResult fr = windowFetchOrders(from, to, page, limit);

        List<OrderYellowRowDto> rows = mapOrdersToYellowRows(fr.getPageOrders());
        if (rows.size() > limit) rows = rows.subList(0, limit);

        return new PageResult<>(page, limit, fr.getTotalRecords(), fr.getTotalPages(), rows);
    }

    /* ========================= RAW & nhóm dữ liệu con ========================= */

    public Map<String, Object> getRaw(Date from, Date to, int page, int limit) throws JsonProcessingException {
        Map<String, String> q = buildQuery(from, to, page, limit);
        return nhanhClient.listOrdersIndex(q);
    }

    private List<OrderYellowRowDto> mapOrdersToYellowRows(List<Map<String, Object>> orders) {
        final int[] stt = {1};

        return orders.stream().flatMap(order -> {
            Long   idNhanh  = asLong(order.get("id"));
            String status   = s(fn(order.get("statusName"), order.get("status")));
            String carrier  = s(fn(order.get("carrierName"), order.get("shippingPartner")));
            String shipCode = s(fn(order.get("carrierCode"), order.get("shipmentCode")));
            String phone    = s(fn(order.get("customerMobile"), order.get("customerPhone")));
            String type     = s(order.get("type"));
            String created  = toDateTimeString(fn(order.get("createdDateTime"),
                    order.get("createdTime"),
                    order.get("createdAt")));

            Long   codPhaiThu    = asLong(fn(order.get("calcTotalMoney"),
                    order.get("grandTotal"),
                    order.get("total")));
            String paymentStatus = s(fn(order.get("paymentStatus"), order.get("paidStatus")));

            String paymentMethod = s(fn(order.get("paymentMethod"), order.get("paymentMethodName")));
            if (isBlank(paymentMethod)) {
                boolean isDelivery = (carrier != null && !carrier.isBlank())
                        || (type != null && type.toLowerCase().contains("giao hàng"));
                paymentMethod = isDelivery ? "COD" : "Tiền mặt";
            }

            List<Map<String, Object>> items = listOfMap(fn(
                    order.get("products"), order.get("items"), order.get("orderItems"), Collections.emptyList()
            ));

            // Đơn không có item -> vẫn trả 1 dòng theo đơn
            if (items.isEmpty()) {
                OrderYellowRowDto row = OrderYellowRowDto.builder()
                        .stt(stt[0]++)
                        .ngay(created)
                        .idNhanh(idNhanh)
                        .soDienThoaiKhach(phone)
                        .maSanPham(null)
                        .size(null)
                        .giaTienBan(null)
                        .kenhThanhToan(paymentMethod)
                        .codPhaiThu(codPhaiThu)
                        .trangThaiThanhToan(paymentStatus)
                        .donViVanChuyen(carrier)
                        .maDonHangVanChuyen(shipCode)
                        .trangThaiTrenNhanh(status)
                        .build();
                return java.util.stream.Stream.of(row);
            }

            // Có item -> 1 dòng / SKU
            final String pm = paymentMethod;
            return items.stream().map(it -> {
                String sku  = s(fn(it.get("productCode"), it.get("sku"), it.get("productId")));
                String size = s(fn(it.get("size"), it.get("variantName")));
                if (isBlank(size)) size = extractSizeFromSku(sku);
                Long price  = asLong(fn(it.get("price"), it.get("sellPrice"), it.get("unitPrice")));

                return OrderYellowRowDto.builder()
                        .stt(stt[0]++)
                        .ngay(created)
                        .idNhanh(idNhanh)
                        .soDienThoaiKhach(phone)
                        .maSanPham(sku)
                        .size(size)
                        .giaTienBan(price)
                        .kenhThanhToan(pm)
                        .codPhaiThu(codPhaiThu)
                        .trangThaiThanhToan(paymentStatus)
                        .donViVanChuyen(carrier)
                        .maDonHangVanChuyen(shipCode)
                        .trangThaiTrenNhanh(status)
                        .build();
            });
        }).collect(Collectors.toList());
    }


    public List<OrderYellowRowDto> getYellowRows(Date from, Date to, int page, int limit) throws JsonProcessingException {
        WindowFetchResult fr = windowFetchOrders(from, to, page, limit);
        return mapOrdersToYellowRows(fr.getPageOrders());
    }

    public List<ProductRowDto> getProducts(Date from, Date to, int page, int limit) throws JsonProcessingException {
        WindowFetchResult fr = windowFetchOrders(from, to, page, limit);
        List<ProductRowDto> out = new ArrayList<>();
        for (Map<String, Object> o : fr.getPageOrders()) {
            String orderId = s(o.get("id"));
            String created = toDateTimeString(fn(o.get("createdDateTime"), o.get("createdTime")));
            String depot   = s(o.get("depotName"));
            String saleCh  = saleChannelName(o);

            List<Map<String, Object>> prods = listOfMap(fn(o.get("products"), o.get("items")));
            if (prods.isEmpty()) {
                out.add(new ProductRowDto(orderId, created, saleCh, depot, null, null, null, null, 0,
                        bd(0), bd(0), bd(0), bd(0)));
            } else {
                for (Map<String, Object> p : prods) {
                    out.add(new ProductRowDto(
                            orderId, created, saleCh, depot,
                            s(fn(p.get("productCode"), p.get("sku"), p.get("productId"))),
                            s(fn(p.get("productName"), p.get("name"))),
                            s(fn(p.get("productBarcode"), p.get("barcode"))),
                            s(fn(p.get("productImage"), p.get("image"))),
                            i(fn(p.get("quantity"), p.get("qty"), 1)),
                            bd(fn(p.get("price"), p.get("sellPrice"), p.get("unitPrice"))),
                            bd(fn(p.get("discount"), p.get("lineDiscount"), 0)),
                            bd(fn(p.get("productMoney"), p.get("lineAmount"), 0)),
                            bd(fn(p.get("avgCost"), 0))
                    ));
                }
            }
        }
        return out;
    }

    public List<PaymentRowDto> getPayments(Date from, Date to, int page, int limit) throws JsonProcessingException {
        WindowFetchResult fr = windowFetchOrders(from, to, page, limit);
        List<PaymentRowDto> out = new ArrayList<>();
        for (Map<String, Object> o : fr.getPageOrders()) {
            BigDecimal subtotal        = bd(o.get("calcTotalMoney"));
            BigDecimal orderDiscount   = bd(o.get("moneyDiscount"));
            BigDecimal shipFeeCustomer = bd(o.get("customerShipFee"));
            BigDecimal grandTotal      = subtotal.add(shipFeeCustomer);

            BigDecimal deposit  = bd(o.get("moneyDeposit"));
            BigDecimal transfer = bd(o.get("moneyTransfer"));
            BigDecimal credit   = bd(o.get("creditAmount"));
            BigDecimal points   = bd(o.get("usedPointAmount"));
            BigDecimal paid     = deposit.add(transfer).add(credit).add(points);
            BigDecimal cod      = grandTotal.subtract(paid).max(BigDecimal.ZERO);

            String paymentStatus = paid.compareTo(grandTotal) >= 0 ? "Đã thanh toán"
                    : paid.compareTo(BigDecimal.ZERO) > 0 ? "Thanh toán một phần" : "Chưa thanh toán";
            String method = transfer.compareTo(BigDecimal.ZERO) > 0 ? "Chuyển khoản"
                    : credit.compareTo(BigDecimal.ZERO) > 0 ? "Công nợ"
                    : paid.compareTo(BigDecimal.ZERO) == 0 ? "COD" : "Khác";

            out.add(new PaymentRowDto(
                    s(o.get("id")),
                    toDateTimeString(fn(o.get("createdDateTime"), o.get("createdTime"))),
                    saleChannelName(o),
                    subtotal, orderDiscount, shipFeeCustomer, grandTotal,
                    deposit, transfer, credit, points, paid, cod, method, paymentStatus
            ));
        }
        return out;
    }

    public List<ShippingRowDto> getShipping(Date from, Date to, int page, int limit) throws JsonProcessingException {
        WindowFetchResult fr = windowFetchOrders(from, to, page, limit);
        List<ShippingRowDto> out = new ArrayList<>();
        for (Map<String, Object> o : fr.getPageOrders()) {
            Map<String, Object> packed = map(o.get("packed"));
            out.add(new ShippingRowDto(
                    s(o.get("id")),
                    s(fn(o.get("statusName"), o.get("status"))),
                    s(o.get("statusCode")),
                    pickShipStatus(s(o.get("statusCode")), packed, s(o.get("sendCarrierDate"))),
                    s(o.get("carrierName")), s(o.get("carrierCode")),
                    bd(o.get("shipFee")), bd(o.get("codFee")), bd(o.get("returnFee")),
                    bd(o.get("overWeightShipFee")), bd(o.get("declaredFee")),
                    s(o.get("sendCarrierDate")), s(o.get("deliveryDate")),
                    packed == null ? null : s(packed.get("datetime"))
            ));
        }
        return out;
    }

    public List<CustomerRowDto> getCustomer(Date from, Date to, int page, int limit) throws JsonProcessingException {
        WindowFetchResult fr = windowFetchOrders(from, to, page, limit);
        List<CustomerRowDto> out = new ArrayList<>();
        for (Map<String, Object> o : fr.getPageOrders()) {
            out.add(new CustomerRowDto(
                    s(o.get("id")),
                    l(o.get("customerId")),
                    s(o.get("customerName")), s(o.get("customerMobile")), s(o.get("customerEmail")),
                    s(o.get("customerAddress")),
                    l(o.get("customerCityId")), s(o.get("customerCity")),
                    l(o.get("customerDistrictId")), s(o.get("customerDistrict")),
                    l(o.get("shipToWardLocationId")), s(o.get("customerWard")),
                    l(o.get("depotId")), s(o.get("depotName")),
                    l(o.get("createdById")), s(o.get("createdByName")),
                    l(o.get("saleId")), s(o.get("saleName")),
                    l(o.get("trafficSourceId")), s(o.get("trafficSourceName")),
                    s(fn(o.get("createdDateTime"), o.get("createdTime")))
            ));
        }
        return out;
    }

    public MetaDto getMeta(Date from, Date to, int page, int limit) throws JsonProcessingException {
        WindowFetchResult fr = windowFetchOrders(from, to, page, limit);
        return new MetaDto(
                uniq(fr.getPageOrders().stream().map(this::saleChannelName).collect(Collectors.toList())),
                uniq(fr.getPageOrders().stream().map(o -> s(o.get("depotName"))).collect(Collectors.toList())),
                uniq(fr.getPageOrders().stream().map(o -> s(o.get("carrierName"))).collect(Collectors.toList())),
                uniq(fr.getPageOrders().stream().map(o -> s(fn(o.get("statusName"), o.get("status")))).collect(Collectors.toList())),
                uniq(fr.getPageOrders().stream().map(o -> pickShipStatus(s(o.get("statusCode")), map(o.get("packed")), s(o.get("sendCarrierDate")))).collect(Collectors.toList())),
                uniq(fr.getPageOrders().stream().map(o -> s(o.get("trafficSourceName"))).collect(Collectors.toList())),
                uniq(fr.getPageOrders().stream().map(o -> s(o.get("createdByName"))).collect(Collectors.toList()))
        );
    }

    public SummaryDto getSummary(Date from, Date to, int page, int limit) throws JsonProcessingException {
        WindowFetchResult fr = windowFetchOrders(from, to, page, limit);

        int count = fr.getPageOrders().size();
        BigDecimal subtotal = bd(0), shipFee = bd(0), grand = bd(0), paid = bd(0), cod = bd(0);
        Map<String, SummaryDto.Group> byShip = new LinkedHashMap<>();

        for (Map<String, Object> o : fr.getPageOrders()) {
            BigDecimal sub = bd(o.get("calcTotalMoney"));
            BigDecimal ship = bd(o.get("customerShipFee"));
            BigDecimal g = sub.add(ship);
            BigDecimal p = bd(o.get("moneyDeposit")).add(bd(o.get("moneyTransfer")))
                    .add(bd(o.get("creditAmount"))).add(bd(o.get("usedPointAmount")));
            BigDecimal c = g.subtract(p).max(BigDecimal.ZERO);
            String s = pickShipStatus(s(o.get("statusCode")), map(o.get("packed")), s(o.get("sendCarrierDate")));

            subtotal = subtotal.add(sub);
            shipFee = shipFee.add(ship);
            grand = grand.add(g);
            paid = paid.add(p);
            cod = cod.add(c);

            SummaryDto.Group gobj = byShip.computeIfAbsent(s, k -> new SummaryDto.Group(0, bd(0), bd(0)));
            gobj.setCount(gobj.getCount() + 1);
            gobj.setGrandTotal(gobj.getGrandTotal().add(g));
            gobj.setCodToCollect(gobj.getCodToCollect().add(c));
        }

        return new SummaryDto(count, subtotal, shipFee, grand, paid, cod, byShip);
    }

    /* ========================= WINDOW FETCH ========================= */

    private WindowFetchResult windowFetchOrders(Date from, Date to, int page, int limit)
            throws JsonProcessingException {

        // CASE 1: không có filter ngày -> dùng phân trang của POS
        if (from == null && to == null) {
            Map<String, String> q = buildQuery(null, null, page, limit);
            Map<String, Object> resp = nhanhClient.listOrdersIndex(q);
            Map<String, Object> data = asMap(resp.get("data"));

            Object ordersObj = (data.get("orders") != null) ? data.get("orders") : data.get("items");
            List<Map<String, Object>> orders = new ArrayList<>();
            if (ordersObj instanceof Map) {
                ((Map<?, ?>) ordersObj).values().forEach(v -> orders.add(asMap(v)));
            } else if (ordersObj instanceof List) {
                for (Object v : (List<?>) ordersObj) if (v instanceof Map) orders.add(asMap(v));
            }

            // ⭐ BỔ SUNG 2 DÒNG NÀY
            int totalRecords = asInt(data.get("totalRecords"), orders.size());
            int totalPages   = asInt(data.get("totalPages"), (int) Math.ceil(totalRecords / (double) limit));

            return new WindowFetchResult(orders, totalRecords, totalPages);
        }

        // CASE 2: có from/to -> tự tính tổng thật + lát cắt trang
        LocalDate f = (from == null) ? MIN_DATE_FOR_ALL : toLocalDate(from);
        LocalDate t = (to   == null) ? LocalDate.now()   : toLocalDate(to);
        if (f.isAfter(t)) { LocalDate tmp = f; f = t; t = tmp; }

        final int startIndex = Math.max(0, (page - 1) * limit);
        final int endIndexExclusive = startIndex + limit;

        List<Map<String, Object>> pageOrders = new ArrayList<>(limit);
        int totalAll = 0;
        int globalIdx = 0;
        final int PER_PAGE = 100;

        LocalDate cur = f;
        while (!cur.isAfter(t)) {
            LocalDate winEnd = cur.plusDays(MAX_DAYS_PER_CALL - 1);
            if (winEnd.isAfter(t)) winEnd = t;

            int wnPage = 1;
            while (true) {
                Map<String, String> q = new LinkedHashMap<>();
                q.put("fromDate", D_FMT.format(java.sql.Date.valueOf(cur)));
                q.put("toDate",   D_FMT.format(plusOneDay(java.sql.Date.valueOf(winEnd))));
                q.put("page",  String.valueOf(wnPage));
                q.put("limit", String.valueOf(PER_PAGE));

                Map<String, Object> resp = nhanhClient.listOrdersIndex(q);
                Map<String, Object> data = asMap(resp.get("data"));

                Object ordersObj = (data.get("orders") != null) ? data.get("orders") : data.get("items");
                List<Map<String, Object>> chunk = new ArrayList<>();
                if (ordersObj instanceof Map) {
                    ((Map<?, ?>) ordersObj).values().forEach(v -> chunk.add(asMap(v)));
                } else if (ordersObj instanceof List) {
                    for (Object v : (List<?>) ordersObj) if (v instanceof Map) chunk.add(asMap(v));
                }

                if (chunk.isEmpty()) break;

                totalAll += chunk.size();

                int localStart = Math.max(0, startIndex - globalIdx);
                int localEnd   = Math.min(chunk.size(), endIndexExclusive - globalIdx);
                if (localStart < localEnd) {
                    pageOrders.addAll(chunk.subList(localStart, localEnd));
                }

                globalIdx += chunk.size();

                if (chunk.size() < PER_PAGE) break;
                wnPage++;
            }

            cur = winEnd.plusDays(1);
        }

        int totalPages = (limit <= 0) ? 0 : (int) Math.ceil(totalAll / (double) limit);
        return new WindowFetchResult(pageOrders, totalAll, totalPages);
    }

    /* ========================= Query build ========================= */

    private Map<String, String> buildQuery(Date from, Date to, int page, int limit) {
        Map<String, String> q = new LinkedHashMap<>();
        if (!(from == null && to == null)) {
            if (from == null || to == null) {
                LocalDate today = LocalDate.now();
                LocalDate f = (from == null) ? MIN_DATE_FOR_ALL : toLocalDate(from);
                LocalDate t = (to   == null) ? today            : toLocalDate(to);
                from = java.sql.Date.valueOf(f);
                to   = java.sql.Date.valueOf(t);
            }
            q.put("fromDate", D_FMT.format(from));
            q.put("toDate",   D_FMT.format(plusOneDay(to)));
        }
        q.put("page",  String.valueOf(page));
        q.put("limit", String.valueOf(limit));
        return q;
    }

    // Parse int an toàn: nhận Number hoặc String; null/invalid → trả về def
    private static int asInt(Object v, int def) {
        if (v == null) return def;
        try {
            if (v instanceof Number n) return n.intValue();
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return def;
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return def;
        }
    }

    // Overload mặc định def = 0
    private static int asInt(Object v) {
        return asInt(v, 0);
    }


    /* ========================= Helpers ========================= */

    private static Date plusOneDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }

    private static LocalDate toLocalDate(Date d) {
        if (d == null) return null;
        if (d instanceof java.sql.Date) return ((java.sql.Date) d).toLocalDate();
        return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String toDateTimeString(Object createdTime) {
        if (createdTime == null) return null;
        try {
            long v = Long.parseLong(String.valueOf(createdTime));
            if (String.valueOf(createdTime).length() <= 10) v = v * 1000;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(v), ZoneId.systemDefault()).format(DT_FMT);
        } catch (Exception ignore) {
            return String.valueOf(createdTime);
        }
    }

    private String saleChannelName(Map<String, Object> o) {
        Integer sc = i(fn(o.get("saleChannel"), o.get("channel")));
        if (sc == null) return "Khác";
        switch (sc) {
            case 1: return "Cửa hàng";
            case 2: return "Online";
            case 3: return "Sàn TMĐT";
            default: return "Khác";
        }
    }

    private String pickShipStatus(String statusCode, Map<String, Object> packed, String sendCarrierDate) {
        if ("Success".equalsIgnoreCase(statusCode))  return "Đã giao";
        if ("Returned".equalsIgnoreCase(statusCode)) return "Đã hoàn";
        if (sendCarrierDate != null && !sendCarrierDate.isBlank()) return "Đang giao";
        if (packed != null && packed.get("datetime") != null)      return "Đã đóng gói";
        return "Mới tạo";
    }

    private static String extractSizeFromSku(String sku) {
        if (isBlank(sku)) return null;
        int i = sku.lastIndexOf('-');
        if (i < 0 || i == sku.length() - 1) return null;
        String cand = sku.substring(i + 1).trim();
        String up = cand.toUpperCase();
        List<String> whitelist = List.of("XS", "S", "M", "L", "XL", "XXL", "2XL", "3XL", "4XL", "F", "FREE", "FREESIZE");
        return whitelist.contains(up) ? up : cand;
    }

    private static Map<String, Object> asMap(Object o) { return (o instanceof Map) ? (Map<String, Object>) o : new LinkedHashMap<>(); }
    private static Map<String, Object> map(Object o)   { return (o instanceof Map) ? (Map<String, Object>) o : null; }
    private static List<Map<String, Object>> listOfMap(Object o) {
        if (!(o instanceof List)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object it : (List<?>) o) if (it instanceof Map) out.add((Map<String, Object>) it);
        return out;
    }
    private static String s(Object o) { return (o == null) ? null : String.valueOf(o); }
    private static Integer i(Object o) { try { return (o == null) ? null : Integer.valueOf(String.valueOf(o)); } catch (Exception e) { return null; } }
    private static Integer i(Object o, int def) { Integer v = i(o); return v == null ? def : v; }
    private static Long l(Object o) { try { return (o == null) ? null : Long.valueOf(String.valueOf(o)); } catch (Exception e) { return null; } }
    private static Long asLong(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof Number) return ((Number) v).longValue();
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) { return null; }
    }
    private static BigDecimal bd(Object o) {
        try { return (o == null || String.valueOf(o).isBlank()) ? BigDecimal.ZERO : new BigDecimal(String.valueOf(o)); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }
    @SuppressWarnings("unchecked") private static <T> T fn(Object... arr) { for (Object o : arr) if (o != null) return (T) o; return null; }
    private static <T> List<T> uniq(List<T> in) { return in.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList()); }

    /* ========================= Window result DTO ========================= */
    @Getter
    private static class WindowFetchResult {
        private final List<Map<String, Object>> pageOrders;
        private final int totalRecords;
        private final int totalPages;

        private WindowFetchResult(List<Map<String, Object>> pageOrders, int totalRecords, int totalPages) {
            this.pageOrders = pageOrders == null ? List.of() : pageOrders;
            this.totalRecords = Math.max(0, totalRecords);
            this.totalPages = Math.max(0, totalPages);
        }
    }
}
