package com.app84soft.check_in.controller;

import com.app84soft.check_in.dto.request.admin.CreateUserByAdminReq;
import com.app84soft.check_in.dto.response.BaseResponse;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/v1/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AuthService authService;

    @PostMapping("/users")
    @Operation(summary = "Admin tạo tài khoản user")
    public ResponseEntity<BaseResponse<User>> createUserByAdmin(@RequestBody @Valid CreateUserByAdminReq req) {
        return ResponseEntity.ok(new BaseResponse<>(authService.createUserByAdmin(req)));
    }
}
