package com.app84soft.check_in.configuration;

import com.app84soft.check_in.dto.nhanh.LoggingClientHttpRequestInterceptor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
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

        RequestConfig rc = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(rc)
                .setConnectionManager(cm)
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.ofSeconds(30))
                .disableCookieManagement()
                .build();

        var httpFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        httpFactory.setConnectTimeout(5_000);
        httpFactory.setReadTimeout(10_000);
        httpFactory.setConnectionRequestTimeout(2_000);

        ClientHttpRequestFactory buffering = new BufferingClientHttpRequestFactory(httpFactory);

        RestTemplate rt = new RestTemplate(buffering);

        // Interceptor nhẹ: gzip + accept json + UA
        rt.getInterceptors().add((req, body, ex) -> {
            var h = req.getHeaders();
            h.putIfAbsent("Accept-Encoding", Collections.singletonList("gzip"));
            h.putIfAbsent("Accept", Collections.singletonList("application/json"));
            h.putIfAbsent("User-Agent", Collections.singletonList("check-in-service/1.0"));
            return ex.execute(req, body);
        });

        // Logging của bạn
        rt.setInterceptors(List.of(logging, rt.getInterceptors().get(0)));
        return rt;
    }
}
