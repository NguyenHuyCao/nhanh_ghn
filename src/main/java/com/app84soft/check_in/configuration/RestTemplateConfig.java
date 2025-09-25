package com.app84soft.check_in.configuration;

import com.app84soft.check_in.dto.nhanh.LoggingClientHttpRequestInterceptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.requestFactory(
                () -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .additionalInterceptors(new LoggingClientHttpRequestInterceptor())
                .connectTimeout(Duration.ofSeconds(10)).readTimeout(Duration.ofSeconds(20)).build();
    }
}
