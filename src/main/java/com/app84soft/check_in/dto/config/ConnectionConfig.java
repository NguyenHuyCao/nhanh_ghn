package com.app84soft.check_in.dto.config;

import lombok.Data;

@Data
public class ConnectionConfig {
    private String apiUrl;
    private int maxRequest = 200;
    private int maxRequestPerHost = 50;
    private long connectTimeout = 30;

}
