package com.app84soft.check_in.security.interceptor;

import com.app84soft.check_in.component.Translator;
import com.app84soft.check_in.dto.constant.ActiveStatus;
import com.app84soft.check_in.dto.constant.RoleType;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.exceptions.BusinessException;
import com.app84soft.check_in.repositories.user.UserRepository;
import com.app84soft.check_in.security.JwtTokenProvider;
import com.app84soft.check_in.security.SecurityContexts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Log4j2
public class AdminInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        BusinessException exception = new BusinessException(Translator.toLocale("login_required"), HttpStatus.UNAUTHORIZED);
        String vendorCode = request.getHeader("Authorization");
        if (StringUtils.isEmpty(vendorCode)) throw exception;

        String[] header = vendorCode.split(" ");
        if (header.length != 2) throw exception;

        String token = header[1];
        if (jwtTokenProvider.validateToken(token)) {
            Integer sub = Integer.valueOf(jwtTokenProvider.getSubIdFromJwt(token));
            User user = userRepository.findById(sub).orElseThrow(() -> exception);
            if (user.getRoleType() != RoleType.ADMIN) throw exception;

            SecurityContexts.newContext(user);
            return true;
        }
        throw exception;
    }
}


