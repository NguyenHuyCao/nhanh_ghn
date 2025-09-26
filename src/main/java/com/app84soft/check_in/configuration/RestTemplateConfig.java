package com.app84soft.check_in.configuration;

import com.app84soft.check_in.dto.nhanh.LoggingClientHttpRequestInterceptor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public LoggingClientHttpRequestInterceptor loggingClientHttpRequestInterceptor() {
        return new LoggingClientHttpRequestInterceptor();
    }

    @Bean
    public RestTemplate pooledRestTemplate(LoggingClientHttpRequestInterceptor logging) {
        var cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(200)
                .setMaxConnPerRoute(50)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .evictExpiredConnections()
                .disableCookieManagement()
                .build();

        var httpFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        httpFactory.setConnectTimeout(5_000);
        httpFactory.setReadTimeout(15_000);
        httpFactory.setConnectionRequestTimeout(2_000);

        ClientHttpRequestFactory buffering = new BufferingClientHttpRequestFactory(httpFactory);

        RestTemplate rt = new RestTemplate(buffering);
        rt.setInterceptors(List.of(logging)); // log request/response
        return rt;
    }
}
