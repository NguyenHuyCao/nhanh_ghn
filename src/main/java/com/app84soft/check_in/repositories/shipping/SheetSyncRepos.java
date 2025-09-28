package com.app84soft.check_in.repositories.shipping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repo trung gian cho các Poller — chỉ là lớp "façade" gọi xuống Store/Jdbc.
 * Thiết kế để KHÔNG phá vỡ API cũ.
 */
public interface SheetSyncRepos {

    /* ========= Watermark ========= */
    long getNhanhWatermark();
    void saveNhanhWatermark(long epochMs);

    LocalDateTime getGhnWatermark();
    void saveGhnWatermark(LocalDateTime t);

    /* ========= UPSERT from Nhanh ========= */

    /** Upsert nhanh_orders từ Map trả về của POS Nhanh */
    void upsertNhanhOrder(Map<String, Object> order);

    /**
     * Upsert nhanh_order_items theo (orderId, items).
     * Đây là overload mới để khớp với NhanhPoller.
     */
    void upsertNhanhItems(long orderId, List<Map<String, Object>> items);

    /**
     * Gộp nhanh vào bảng sheet_rows (nếu bạn dùng bảng này).
     * Tham số thứ 3 có thể để null (GHN chưa có).
     * Không bắt buộc phải làm gì — có thể no-op nếu bạn không dùng sheet_rows.
     */
    void upsertSheetRow(Map<String, Object> nhanhOrder,
                        Map<String, Object> nhanhItem,
                        Map<String, Object> ghnOrNull);

    /* ========= UPSERT from GHN ========= */

    /**
     * Upsert dữ liệu GHN từ Map (dùng trong GhnPoller).
     * Có thể no-op nếu bạn đã enrich bằng service khác.
     */
    void upsertGhnOrder(Map<String, Object> ghnMap);
}
