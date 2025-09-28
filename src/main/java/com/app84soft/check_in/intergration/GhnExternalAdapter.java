// src/main/java/com/app84soft/check_in/intergration/GhnExternalAdapter.java
package com.app84soft.check_in.intergration;

import com.app84soft.check_in.other_service.ghn.GhnSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GhnExternalAdapter implements ExternalGhnClient {
    private final GhnSheetService ghnSheetService;

    @Override
    public GhnOrder getOrder(String orderCode) {
        var w = ghnSheetService.one(orderCode); // bạn đã có WhiteRowDto
        if (w == null) return null;
        return new GhnOrder(
                w.getOrderCode(),
                w.getClientOrderCode(),
                w.getShipStatus(),
                w.getDeliveredAt(),
                w.getShipFee(),
                w.getCodAmount(),
                w.getReturnNote()
        );
    }
}
