package com.app84soft.check_in.services.sheet;

import com.app84soft.check_in.dto.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocalSheetQueryService {

    private final JdbcTemplate jdbc;

    private static final int MAX_LIMIT = 20_000;

    // FROM/JOIN chuẩn: luôn có LEFT JOIN go để có thể lọc theo go.is_pr
    private static final String BASE_FROM =
            " FROM nhanh_order_items noi " +
                    " JOIN nhanh_orders o ON o.id = noi.order_id " +
                    " LEFT JOIN ghn_orders go ON go.order_code = o.carrier_code ";

    /** Truy vấn dữ liệu đã cache trong DB và trả về PageResult chuẩn */
    public PageResult<Map<String,Object>> query(LocalDate from, LocalDate to, int page, int limit) {
        if (limit <= 0) limit = 20;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;
        int offset = Math.max(0, (page - 1) * limit);

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (from != null) where.append(" AND o.created_at >= '").append(from).append(" 00:00:00' ");
        if (to   != null) where.append(" AND o.created_at <  '").append(to.plusDays(1)).append(" 00:00:00' ");

        // ẨN GHN PR: vẫn giữ null (chưa có GHN) + các đơn không PR
        where.append(" AND (go.is_pr = 0 OR go.is_pr IS NULL) ");

        // 1) COUNT
        long total = jdbc.queryForObject("SELECT COUNT(*) " + BASE_FROM + where, Long.class);

        // 2) Lấy khóa trang (có LEFT JOIN để dùng được điều kiện go.is_pr)
        String keySql =
                "SELECT noi.id AS item_id " +
                        " FROM nhanh_order_items noi FORCE INDEX (idx_noi_order_id_id) " +
                        " JOIN nhanh_orders o FORCE INDEX (idx_no_created_at_id) ON o.id = noi.order_id " +
                        " LEFT JOIN ghn_orders go ON go.order_code = o.carrier_code " +
                        where +
                        " ORDER BY o.created_at DESC, noi.id ASC " +
                        " LIMIT ? OFFSET ?";

        List<Long> itemIds = jdbc.query(keySql, (rs, i) -> rs.getLong(1), limit, offset);
        if (itemIds.isEmpty()) {
            return new PageResult<>(page, limit, total, (int)Math.ceil(total/(double)limit), List.of());
        }

        // 3) Lấy dữ liệu chi tiết
        String inClause = String.join(",", java.util.Collections.nCopies(itemIds.size(), "?"));
        String dataSql =
                "SELECT " +
                        "  noi.id               AS item_id, " +
                        "  o.id                 AS nhanhOrderId, " +
                        "  o.created_at         AS createdAt, " +
                        "  o.customer_phone     AS customerPhone, " +
                        "  noi.sku              AS sku, " +
                        "  noi.`size`           AS `size`, " +
                        "  noi.unit_price       AS unitPrice, " +
                        "  noi.quantity         AS quantity, " +
                        "  noi.discount_total   AS discountTotal, " +
                        "  noi.deposit_alloc    AS depositAlloc, " +
                        "  noi.transfer_alloc   AS transferAlloc, " +
                        "  noi.revenue_item     AS revenueItem, " +
                        "  o.payment_channel    AS paymentChannel, " +
                        "  o.cod_to_collect     AS codToCollect, " +
                        "  o.carrier            AS carrier, " +
                        "  o.carrier_code       AS carrierOrderCode, " +
                        "  o.`status`           AS nhanhStatus, " +
                        "  go.order_code        AS ghnOrderCode, " +
                        "  go.client_order_code AS ghnClientOrderCode, " +
                        "  go.delivered_at      AS ghnDeliveredAt, " +
                        "  go.ship_fee          AS ghnShipFee, " +
                        "  go.cod_amount        AS ghnCodAmount, " +
                        "  go.ship_status       AS ghnShipStatus, " +
                        "  go.return_note       AS ghnReturnNote " +
                        BASE_FROM +
                        " WHERE noi.id IN (" + inClause + ") " +
                        " ORDER BY o.created_at DESC, noi.id ASC";

        List<Map<String,Object>> items = jdbc.query(dataSql, (rs,i) -> mapRow(rs), itemIds.toArray());
        int totalPages = (limit <= 0) ? 0 : (int)Math.ceil(total / (double)limit);
        return new PageResult<>(page, limit, total, totalPages, items);
    }

    public long countAllOrders() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM nhanh_orders", Long.class);
        return n == null ? 0 : n;
    }

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", rs.getLong("item_id"));

        Timestamp created = rs.getTimestamp("createdAt");
        m.put("createdAt", created == null ? null : created.toLocalDateTime().toString().replace('T', ' '));

        m.put("nhanhOrderId", rs.getLong("nhanhOrderId"));
        m.put("customerPhone", rs.getString("customerPhone"));
        m.put("sku", rs.getString("sku"));
        m.put("size", rs.getString("size"));
        m.put("unitPrice", rs.getLong("unitPrice"));
        m.put("paymentChannel", rs.getString("paymentChannel"));
        m.put("codToCollect", rs.getLong("codToCollect"));
        m.put("carrier", rs.getString("carrier"));
        m.put("carrierOrderCode", rs.getString("carrierOrderCode"));
        m.put("nhanhStatus", rs.getString("nhanhStatus"));
        m.put("ghnOrderCode", rs.getString("ghnOrderCode"));
        m.put("ghnClientOrderCode", rs.getString("ghnClientOrderCode"));

        Timestamp d = rs.getTimestamp("ghnDeliveredAt");
        m.put("ghnDeliveredAt", d == null ? null : d.toLocalDateTime());

        Object fee = rs.getObject("ghnShipFee");
        m.put("ghnShipFee", fee == null ? null : rs.getLong("ghnShipFee"));

        Object cod = rs.getObject("ghnCodAmount");
        m.put("ghnCodAmount", cod == null ? null : rs.getLong("ghnCodAmount"));

        m.put("ghnShipStatus", rs.getString("ghnShipStatus"));
        m.put("ghnReturnNote", rs.getString("ghnReturnNote"));

        m.put("quantity", rs.getInt("quantity"));
        Object dsc = rs.getObject("discountTotal");
        m.put("discountTotal", dsc == null ? null : rs.getLong("discountTotal"));
        Object da = rs.getObject("depositAlloc");
        m.put("depositAlloc", da == null ? null : rs.getLong("depositAlloc"));
        Object ta = rs.getObject("transferAlloc");
        m.put("transferAlloc", ta == null ? null : rs.getLong("transferAlloc"));
        Object rev = rs.getObject("revenueItem");
        m.put("revenueItem", rev == null ? null : rs.getLong("revenueItem"));

        return m;
    }
}
