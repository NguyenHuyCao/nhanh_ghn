package com.app84soft.check_in.dto.nhanh;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nhanh.api")
public class NhanhProperties {
    private String baseUrl;
    private String appId;
    private String businessId;
    private String staticToken;
    private long   tokenCacheSeconds = 3500;
}
