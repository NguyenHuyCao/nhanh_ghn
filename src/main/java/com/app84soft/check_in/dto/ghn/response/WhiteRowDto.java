package com.app84soft.check_in.dto.ghn.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class WhiteRowDto {
    private String orderCode;
    private String clientOrderCode;
    private LocalDateTime deliveredAt;
    private Long shipFee;
    private Long codAmount;
    private String shipStatus;
    private String returnNote;

    private LocalDateTime bankCollectedAt;
    private Long bankAmount;
}
