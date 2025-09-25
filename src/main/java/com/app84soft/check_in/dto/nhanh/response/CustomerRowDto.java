package com.app84soft.check_in.dto.nhanh.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRowDto {
    private String orderId;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String address;
    private Long provinceId;
    private String province;
    private Long districtId;
    private String district;
    private Long wardId;
    private String ward;
    private Long depotId;
    private String depotName;
    private Long createdById;
    private String createdBy;
    private Long saleId;
    private String saleName;
    private Long trafficSourceId;
    private String trafficSourceName;
    private String createdDateTime;
}
