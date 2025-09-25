package com.app84soft.check_in.controller;

import com.app84soft.check_in.dto.request.auth.LoginReq;
import com.app84soft.check_in.dto.request.auth.RegisterReq;
import com.app84soft.check_in.dto.response.BaseResponse;
import com.app84soft.check_in.dto.response.user.UserLoginRes;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập (public)")
    public ResponseEntity<BaseResponse<UserLoginRes>> login(@RequestBody @Valid LoginReq req) {
        return ResponseEntity.ok(new BaseResponse<>(authService.login(req)));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản (public)")
    public ResponseEntity<BaseResponse<User>> register(@RequestBody @Valid RegisterReq req) {
        return ResponseEntity.ok(new BaseResponse<>(authService.register(req)));
    }
}

