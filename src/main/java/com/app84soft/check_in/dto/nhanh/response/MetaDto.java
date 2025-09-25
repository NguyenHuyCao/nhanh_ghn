package com.app84soft.check_in.dto.nhanh.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetaDto {
    private List<String> saleChannels;
    private List<String> depots;
    private List<String> carriers;
    private List<String> statusNames;
    private List<String> shippingStatuses;
    private List<String> trafficSources;
    private List<String> creators;
}
