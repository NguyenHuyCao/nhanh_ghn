package com.app84soft.check_in.configuration;

import com.app84soft.check_in.component.converter.EnumConverterFactory;
import com.app84soft.check_in.security.interceptor.AdminInterceptor;
import com.app84soft.check_in.security.interceptor.TrackLogRequestInterceptor;
import com.app84soft.check_in.security.rate_limit.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.*;


@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TrackLogRequestInterceptor trackLogRequestInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        final long MAX_AGE_SECS = 3600;
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("HEAD", "OPTIONS", "GET", "POST", "PUT", "PATCH", "DELETE")
                .maxAge(MAX_AGE_SECS);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
        registry.addInterceptor(trackLogRequestInterceptor).addPathPatterns("/**");
        registry.addInterceptor(adminInterceptor).addPathPatterns("/api/admin/v*/**")
                .excludePathPatterns("/api/admin/v*/auth/refresh", "/api/admin/v*/auth/logout");
    }

    @Override
    public void configureContentNegotiation(@NotNull ContentNegotiationConfigurer configurer) {
        WebMvcConfigurer.super.configureContentNegotiation(configurer);
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new EnumConverterFactory());
    }
}