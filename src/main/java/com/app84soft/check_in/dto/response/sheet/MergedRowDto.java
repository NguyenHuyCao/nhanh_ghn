package com.app84soft.check_in.dto.response.sheet;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MergedRowDto {
    // ===== Nhanh (POS) =====
    private Integer seq;               // formerly: stt
    private String  createdAt;         // formerly: ngay
    private Long    nhanhOrderId;      // formerly: idNhanh
    private String  customerPhone;     // formerly: soDienThoaiKhach
    private String  sku;               // formerly: maSanPham
    private String  size;              // formerly: size
    private Long    unitPrice;         // formerly: giaTienBan
    private String  paymentChannel;    // formerly: kenhThanhToan
    private Long    codToCollect;      // formerly: codPhaiThu
    private String  paymentStatus;     // formerly: trangThaiThanhToan
    private String  carrier;           // formerly: donViVanChuyen
    private String  carrierOrderCode;  // formerly: maDonHangVanChuyen
    private String  nhanhStatus;       // formerly: trangThaiTrenNhanh

    // ===== GHN (enriched) =====
    private String        ghnOrderCode;
    private String        ghnClientOrderCode;
    private LocalDateTime ghnDeliveredAt;
    private Long          ghnShipFee;
    private Long          ghnCodAmount;
    private String        ghnShipStatus;
    private String        ghnReturnNote;
}
