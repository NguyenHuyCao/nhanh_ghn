package com.app84soft.check_in.configuration;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;


@Configuration
public class SwaggerConfig {

    @Value("${system.backend.url}")
    private String BACKEND_URL;

    @Bean
    public GroupedOpenApi userGroupApi() {
        return GroupedOpenApi.builder()
                .group("user-api")
                .displayName("USER API")
                .pathsToMatch("/api/v*/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminGroupApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .displayName("ADMIN API")
                .pathsToMatch("/api/admin/v*/**")
                .build();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()))
                .info(new Info().title("API check in")
                        .description("Api check in")
                        .version("1.0"))
                .addServersItem(new Server().url(BACKEND_URL).description("Generated server URL"));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }

}
