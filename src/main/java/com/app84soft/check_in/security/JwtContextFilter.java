package com.app84soft.check_in.security;

import com.app84soft.check_in.repositories.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtContextFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtContextFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authz = request.getHeader("Authorization");
        if (authz != null && authz.startsWith("Bearer ")) {
            String token = authz.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                try {
                    Integer userId = Integer.valueOf(jwtTokenProvider.getSubIdFromJwt(token));
                    userRepository.findById(userId).ifPresent(SecurityContexts::newContext);
                } catch (Exception ignored) {  }
            }
        }
        filterChain.doFilter(request, response);
    }
}

