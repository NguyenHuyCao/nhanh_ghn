package com.app84soft.check_in.dto.nhanh.response;

import lombok.Builder;
import lombok.Data;

/** Một dòng giống bảng bạn chụp */
@Data
@Builder
public class OrderSheetRowDto {
    private int stt;
    private String ngay;
    private Long idNhanh;
    private String soDienThoaiKhach;
    private String kenhThanhToan;
    private String maSanPham;
    private String donViVanChuyen;
    private String trangThaiTrenNhanh;
    private Long giaTienBan;
    private String maDonHangVanChuyen;
    private String orderDate;
    private String nhanhId;
    private String customerPhone;
    private String productCode;
    private String size;
    private Long salePrice;
    private String paymentMethod;
    private String carrierName;
    private String carrierCode;
    private String status;
    private String collectDate;
    private Long shipFee;
    private Long codCollected;
}
