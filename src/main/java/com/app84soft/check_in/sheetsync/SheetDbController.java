package com.app84soft.check_in.sheetsync;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/v1/sheet-db")
@RequiredArgsConstructor
public class SheetDbController {

    private final NamedParameterJdbcTemplate jdbc;

    @GetMapping("/merge")
    public Map<String,Object> merge(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ){
        var p = new MapSqlParameterSource()
                .addValue("from", from==null? null : from.atStartOfDay())
                .addValue("to",   to==null?   null : to.plusDays(1).atStartOfDay())
                .addValue("off",  (page-1)*limit)
                .addValue("lim",  limit);

        String where = "";
        if (from!=null && to!=null) where = "WHERE created_at >= :from AND created_at < :to";

        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_rows "+where, p, Integer.class);
        List<Map<String,Object>> items = jdbc.queryForList("""
      SELECT nhanh_order_id  AS nhanhOrderId,
             created_at      AS createdAt,
             customer_phone  AS customerPhone,
             sku, size, unit_price AS unitPrice,
             payment_channel AS paymentChannel,
             cod_to_collect  AS codToCollect,
             payment_status  AS paymentStatus,
             carrier, carrier_order_code AS carrierOrderCode,
             nhanh_status    AS nhanhStatus,
             ghn_order_code  AS ghnOrderCode,
             ghn_client_code AS ghnClientOrderCode,
             ghn_delivered_at AS ghnDeliveredAt,
             ghn_ship_fee     AS ghnShipFee,
             ghn_cod_amount   AS ghnCodAmount,
             ghn_ship_status  AS ghnShipStatus,
             ghn_return_note  AS ghnReturnNote
      FROM sheet_rows
      %s
      ORDER BY created_at, nhanh_order_id, sku
      LIMIT :lim OFFSET :off
    """.formatted(where), p);

        return Map.of("code",200, "data", Map.of(
                "total", total==null?0:total,
                "page", page,
                "items", items
        ));
    }
}
