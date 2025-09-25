package com.app84soft.check_in.dto.ghn;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ghn")
public class GhnProperties {
    private String baseUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api";
    private String token;
    private Long shopId;
}

