package com.app84soft.check_in.configuration;

import com.app84soft.check_in.security.JwtAuthenticationEntryPoint;
import com.app84soft.check_in.security.JwtContextFilter;
import com.app84soft.check_in.security.SecurityContextCleanupFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtContextFilter jwtContextFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint unauthorizedHandler,
                          JwtContextFilter jwtContextFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtContextFilter = jwtContextFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextCleanupFilter securityContextCleanupFilter) throws Exception {
        http
                .cors(c -> c.configure(http))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health", "/actuator/info",
                                "/webjars/**", "/static/**"
                        ).permitAll()
                        .requestMatchers("/api/v*/auth/**", "/api/admin/v*/auth/**").permitAll()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedHandler));

        http.addFilterBefore(jwtContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

