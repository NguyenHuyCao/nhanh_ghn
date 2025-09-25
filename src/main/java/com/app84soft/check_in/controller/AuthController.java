package com.app84soft.check_in.controller;

import com.app84soft.check_in.dto.request.auth.LoginReq;
import com.app84soft.check_in.dto.request.auth.RegisterReq;
import com.app84soft.check_in.dto.response.BaseResponse;
import com.app84soft.check_in.dto.response.user.UserLoginRes;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.repositories.user.UserRepository;
import com.app84soft.check_in.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập (public)")
    public ResponseEntity<BaseResponse<Map<String, String>>> login(@RequestBody @Valid LoginReq req,
            HttpServletRequest http) {
        var userRes = authService.login(req); // đang trả về thông tin user + access token
        var user = userRepository.findById(userRes.getId()).orElseThrow();
        var rt = authService.issueRefreshToken(user, http.getHeader("User-Agent"),
                http.getRemoteAddr());
        return ResponseEntity.ok(new BaseResponse<>(
                Map.of("access_token", userRes.getToken(), "refresh_token", rt.getToken())));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản (public)")
    public ResponseEntity<BaseResponse<User>> register(@RequestBody @Valid RegisterReq req) {
        return ResponseEntity.ok(new BaseResponse<>(authService.register(req)));
    }
}

