package com.app84soft.check_in.other_service.sheet;

import com.app84soft.check_in.dto.ghn.response.WhiteRowDto;
import com.app84soft.check_in.dto.nhanh.response.OrderYellowRowDto;
import com.app84soft.check_in.dto.response.PageResult;
import com.app84soft.check_in.dto.response.sheet.MergedRowDto;
import com.app84soft.check_in.other_service.ghn.GhnSheetService;
import com.app84soft.check_in.other_service.nhanh.NhanhSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MergeSheetService {

    private final NhanhSheetService nhanhSheet;
    private final GhnSheetService   ghnSheet;

    /** broaden GHN window to avoid missing late/early deliveries */
    private static final int GHN_FROM_PADDING_DAYS = 7;
    private static final int GHN_TO_PADDING_DAYS   = 14;

    public PageResult<MergedRowDto> merge(LocalDate from, LocalDate to, int page, int limit)
            throws com.fasterxml.jackson.core.JsonProcessingException {

        // 1) Page from Nhanh (filtered by created-date in Nhanh)
        var nhanhPage = nhanhSheet.getYellowPage(
                from == null ? null : java.sql.Date.valueOf(from),
                to   == null ? null : java.sql.Date.valueOf(to),
                page, limit
        );
        var nhanhRows = nhanhPage.getItems(); // List<OrderYellowRowDto>

        // 2) GHN white sheet with expanded window (for fast index)
        LocalDate fG = (from == null) ? null : from.minusDays(GHN_FROM_PADDING_DAYS);
        LocalDate tG = (to   == null) ? null : to.plusDays(GHN_TO_PADDING_DAYS);
        var ghnPage = ghnSheet.white(fG, tG, 1, Math.max(limit * 5, 500));

        // 3) Index GHN orders (by order_code & client_order_code)
        Map<String, WhiteRowDto> byOrderCode  = new HashMap<>();
        Map<String, WhiteRowDto> byClientCode = new HashMap<>();
        for (WhiteRowDto w : ghnPage.items) {
            if (w.getOrderCode() != null)       byOrderCode.putIfAbsent(w.getOrderCode(), w);
            if (w.getClientOrderCode() != null) byClientCode.putIfAbsent(w.getClientOrderCode(), w);
        }

        // 4) Detail fallback cache
        Map<String, WhiteRowDto> detailCache = new ConcurrentHashMap<>();

        List<MergedRowDto> merged = nhanhRows.stream().map(n -> {
            WhiteRowDto g = null;

            String carrier   = nullToEmpty(n.getDonViVanChuyen());
            String orderCode = nullToEmpty(n.getMaDonHangVanChuyen());

            // detect GHN either by normalized name or code prefix (NVS…)
            if (isCarrierGHN(carrier, orderCode) && !orderCode.isBlank()) {
                // fast index
                g = byOrderCode.get(orderCode);
                if (g == null && n.getIdNhanh() != null) {
                    g = byClientCode.get(String.valueOf(n.getIdNhanh()));
                }
                // detail fallback
                if (g == null) {
                    g = detailCache.computeIfAbsent(orderCode, oc -> {
                        try { return ghnSheet.one(oc); } catch (Exception ignore) { return null; }
                    });
                }
            }

            return MergedRowDto.builder()
                    // --- map Nhanh (VI -> EN field names) ---
                    .seq(n.getStt())
                    .createdAt(n.getNgay())
                    .nhanhOrderId(n.getIdNhanh())
                    .customerPhone(n.getSoDienThoaiKhach())
                    .sku(n.getMaSanPham())
                    .size(n.getSize())
                    .unitPrice(n.getGiaTienBan())
                    .paymentChannel(n.getKenhThanhToan())
                    .codToCollect(n.getCodPhaiThu())
                    .paymentStatus(n.getTrangThaiThanhToan())
                    .carrier(n.getDonViVanChuyen())
                    .carrierOrderCode(n.getMaDonHangVanChuyen())
                    .nhanhStatus(n.getTrangThaiTrenNhanh())
                    // --- enrich GHN ---
                    .ghnOrderCode(g == null ? null : g.getOrderCode())
                    .ghnClientOrderCode(g == null ? null : g.getClientOrderCode())
                    .ghnDeliveredAt(g == null ? null : g.getDeliveredAt())
                    .ghnShipFee(g == null ? null : g.getShipFee())
                    .ghnCodAmount(g == null ? null : g.getCodAmount())
                    .ghnShipStatus(g == null ? null : g.getShipStatus())
                    .ghnReturnNote(g == null ? null : g.getReturnNote())
                    .build();
        }).collect(Collectors.toList());

        // keep total/page from Nhanh for stable pagination
        return new PageResult<>(
                nhanhPage.getPage(),
                nhanhPage.getLimit(),
                nhanhPage.getTotal(),
                nhanhPage.getTotalPages(),
                merged
        );
    }

    /* ================= Helpers ================= */

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    /**
     * GHN detection:
     * - normalize carrier name (strip accents, lowercase, alnum only) then search for "ghn"/"giaohangnhanh"
     * - OR check code prefix "NVS"
     */
    private static boolean isCarrierGHN(String carrier, String orderCode) {
        if (carrier == null) carrier = "";
        String normalized = Normalizer.normalize(carrier, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")   // strip accents
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "");

        boolean byName = normalized.contains("ghn") || normalized.contains("giaohangnhanh");
        boolean byCode = orderCode != null && orderCode.toUpperCase().startsWith("NVS");

        return byName || byCode;
    }
}
