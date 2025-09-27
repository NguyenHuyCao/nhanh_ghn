package com.app84soft.check_in.other_service.sheet;

import com.app84soft.check_in.dto.ghn.response.WhiteRowDto;
import com.app84soft.check_in.dto.nhanh.response.OrderYellowRowDto;
import com.app84soft.check_in.dto.response.PageResult;
import com.app84soft.check_in.dto.response.sheet.MergedRowDto;
import com.app84soft.check_in.other_service.ghn.GhnSheetService;
import com.app84soft.check_in.other_service.nhanh.NhanhSheetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MergeSheetService {

    private final NhanhSheetService nhanhSheet;
    private final GhnSheetService   ghnSheet;

    private static final int GHN_FROM_PADDING_DAYS = 7;
    private static final int GHN_TO_PADDING_DAYS   = 14;

    public PageResult<MergedRowDto> merge(LocalDate from, LocalDate to, int page, int limit)
            throws com.fasterxml.jackson.core.JsonProcessingException {

        ExecutorService es = Executors.newFixedThreadPool(2);

        // chạy song song
        CompletableFuture<PageResult<OrderYellowRowDto>> nhanhF = CompletableFuture.supplyAsync(() ->
        {
            try {
                return nhanhSheet.getYellowPage(
                        from == null ? null : java.sql.Date.valueOf(from),
                        to   == null ? null : java.sql.Date.valueOf(to),
                        page, limit
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, es);

        LocalDate fG = (from == null) ? null : from.minusDays(GHN_FROM_PADDING_DAYS);
        LocalDate tG = (to   == null) ? null : to.plusDays(GHN_TO_PADDING_DAYS);

        CompletableFuture<GhnSheetService.Page<WhiteRowDto>> ghnF = CompletableFuture.supplyAsync(() ->
                ghnSheet.white(fG, tG, 1, Math.max(limit * 5, 500)), es);

        PageResult<OrderYellowRowDto> nhanhPage = nhanhF.join();
        GhnSheetService.Page<WhiteRowDto> ghnPage = ghnF.join();
        es.shutdown();

        // index GHN để join nhanh
        Map<String, WhiteRowDto> byOrderCode  = new HashMap<>();
        Map<String, WhiteRowDto> byClientCode = new HashMap<>();
        for (WhiteRowDto w : ghnPage.items) {
            if (w.getOrderCode() != null)       byOrderCode.putIfAbsent(w.getOrderCode(), w);
            if (w.getClientOrderCode() != null) byClientCode.putIfAbsent(w.getClientOrderCode(), w);
        }

        // fallback detail (có cache trong GhnSheetService, nên an toàn)
        Map<String, WhiteRowDto> detailCache = new ConcurrentHashMap<>();

        List<MergedRowDto> merged = nhanhPage.getItems().stream().map(n -> {
            WhiteRowDto g = null;

            String carrier   = safe(n.getDonViVanChuyen());
            String orderCode = safe(n.getMaDonHangVanChuyen());

            if (isCarrierGHN(carrier)) {
                // 1) match bằng order_code (nếu Nhanh đang lưu trùng)
                g = byOrderCode.get(orderCode);

                // 2) fallback bằng client_order_code == nhanhOrderId
                if (g == null && n.getIdNhanh() != null) {
                    g = byClientCode.get(String.valueOf(n.getIdNhanh()));
                }

                // 3) fallback gọi detail (khi có mã; GhnSheetService đã cache + timeout)
                if (g == null && !orderCode.isBlank()) {
                    try {
                        g = detailCache.computeIfAbsent(orderCode, oc -> {
                            try { return ghnSheet.one(oc); } catch (Exception e) { return null; }
                        });
                    } catch (Exception ignore) { /* noop */ }
                }
            }

            return MergedRowDto.builder()
                    // Nhanh
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
                    // GHN
                    .ghnOrderCode(g == null ? null : g.getOrderCode())
                    .ghnClientOrderCode(g == null ? null : g.getClientOrderCode())
                    .ghnDeliveredAt(g == null ? null : g.getDeliveredAt())
                    .ghnShipFee(g == null ? null : g.getShipFee())
                    .ghnCodAmount(g == null ? null : g.getCodAmount())
                    .ghnShipStatus(g == null ? null : g.getShipStatus())
                    .ghnReturnNote(g == null ? null : g.getReturnNote())
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(
                nhanhPage.getPage(),
                nhanhPage.getLimit(),
                nhanhPage.getTotal(),
                nhanhPage.getTotalPages(),
                merged
        );
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /** nhận diện GHN theo tên hãng (ổn định hơn NVS…) */
    private static boolean isCarrierGHN(String carrier) {
        if (carrier == null) carrier = "";
        String normalized = Normalizer.normalize(carrier, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","")
                .toLowerCase().replaceAll("[^a-z0-9]+","");
        return normalized.contains("ghn") || normalized.contains("giaohangnhanh");
    }
}
