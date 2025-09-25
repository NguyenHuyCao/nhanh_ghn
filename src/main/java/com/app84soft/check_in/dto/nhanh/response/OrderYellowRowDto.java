package com.app84soft.check_in.dto.nhanh.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderYellowRowDto {
    private int stt;
    private String ngay;
    private Long idNhanh;
    private String soDienThoaiKhach;
    private String maSanPham;
    private String size;
    private Long giaTienBan;
    private String kenhThanhToan;
    private Long codPhaiThu;
    private String trangThaiThanhToan;
    private String donViVanChuyen;
    private String maDonHangVanChuyen;
    private String trangThaiTrenNhanh;
}
