package com.app84soft.check_in.dto.ghn;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ghn.api")
public class GhnProperties {
    private String baseUrl;
    private String token;
    private Long shopId;
}


