package com.app84soft.check_in.dto.nhanh.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WindowFetchResult {
    private List<Map<String, Object>> pageOrders;
    private int totalRecords;
    private int totalPages;
}
